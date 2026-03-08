package com.vamigo.ride;

import com.vamigo.exceptions.AlreadyBookedException;
import com.vamigo.exceptions.BookingNotFoundException;
import com.vamigo.exceptions.CannotBookException;
import com.vamigo.exceptions.CannotBookOwnRideException;
import com.vamigo.exceptions.ExternalRideNotBookableException;
import com.vamigo.exceptions.InsufficientSeatsException;
import com.vamigo.exceptions.InvalidBookingSegmentException;
import com.vamigo.exceptions.InvalidBookingTransitionException;
import com.vamigo.exceptions.NoSuchRideException;
import com.vamigo.exceptions.NotRideDriverException;
import com.vamigo.exceptions.RideNotBookableException;
import com.vamigo.ride.dto.BookRideRequest;
import com.vamigo.ride.dto.BookingResponseDto;
import com.vamigo.ride.event.BookingCancelledEvent;
import com.vamigo.ride.event.BookingConfirmedEvent;
import com.vamigo.ride.event.BookingRejectedEvent;
import com.vamigo.ride.event.BookingRequestedEvent;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.capability.CapabilityService;
import com.vamigo.user.exception.NoSuchUserException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final RideRepository rideRepository;
    private final RideBookingRepository bookingRepository;
    private final UserAccountRepository userAccountRepository;
    private final BookingMapper bookingMapper;
    private final BookingResponseEnricher bookingResponseEnricher;
    private final CapabilityService capabilityService;
    private final ApplicationEventPublisher eventPublisher;

    public BookingServiceImpl(RideRepository rideRepository,
                               RideBookingRepository bookingRepository,
                               UserAccountRepository userAccountRepository,
                               BookingMapper bookingMapper,
                               BookingResponseEnricher bookingResponseEnricher,
                               CapabilityService capabilityService,
                               ApplicationEventPublisher eventPublisher) {
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
        this.userAccountRepository = userAccountRepository;
        this.bookingMapper = bookingMapper;
        this.bookingResponseEnricher = bookingResponseEnricher;
        this.capabilityService = capabilityService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public BookingResponseDto createBooking(Long rideId, Long passengerId, BookRideRequest request) {
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new NoSuchRideException(rideId));

        if (ride.getSource() != RideSource.INTERNAL) {
            throw new ExternalRideNotBookableException(rideId);
        }

        if (ride.getDriver().getId().equals(passengerId)) {
            throw new CannotBookOwnRideException(rideId);
        }

        if (!capabilityService.canBook(passengerId)) {
            throw new CannotBookException(passengerId);
        }

        if (bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(rideId, passengerId, ACTIVE_STATUSES)) {
            throw new AlreadyBookedException(rideId, passengerId);
        }

        UserAccount passenger = userAccountRepository.findById(passengerId)
                .orElseThrow(() -> new NoSuchUserException(passengerId));

        if (ride.getRideStatus() != RideStatus.OPEN) {
            throw new RideNotBookableException(rideId, ride.getRideStatus().name());
        }

        RideStop boardStop = findStop(ride, request.boardStopOsmId());
        RideStop alightStop = findStop(ride, request.alightStopOsmId());

        if (boardStop.getStopOrder() >= alightStop.getStopOrder()) {
            throw new InvalidBookingSegmentException(rideId,
                    "Board stop must come before alight stop");
        }

        int available = ride.getAvailableSeatsForSegment(
                boardStop.getStopOrder(), alightStop.getStopOrder());
        if (available < request.seatCount()) {
            throw new InsufficientSeatsException(rideId, request.seatCount(), available);
        }

        BookingStatus initialStatus = ride.isAutoApprove()
                ? BookingStatus.CONFIRMED
                : BookingStatus.PENDING;

        RideBooking booking = RideBooking.builder()
                .ride(ride)
                .passenger(passenger)
                .boardStop(boardStop)
                .alightStop(alightStop)
                .status(initialStatus)
                .seatCount(request.seatCount())
                .proposedPrice(request.proposedPrice())
                .bookedAt(Instant.now())
                .resolvedAt(initialStatus == BookingStatus.CONFIRMED ? Instant.now() : null)
                .build();

        bookingRepository.save(booking);
        ride.setLastModified(Instant.now());
        rideRepository.save(ride);

        Long driverId = ride.getDriver().getId();
        if (initialStatus == BookingStatus.PENDING) {
            eventPublisher.publishEvent(new BookingRequestedEvent(
                    booking.getId(), rideId, passengerId, driverId));
        } else {
            eventPublisher.publishEvent(new BookingConfirmedEvent(
                    booking.getId(), rideId, passengerId, driverId));
        }

        BookingResponseDto dto = bookingMapper.toResponseDto(booking);
        return bookingResponseEnricher.enrich(booking, dto);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponseDto getBooking(Long rideId, Long bookingId, Long userId) {
        RideBooking booking = findBookingForRide(rideId, bookingId);

        Long driverId = booking.getRide().getDriver().getId();
        Long passengerId = booking.getPassenger().getId();
        if (!userId.equals(driverId) && !userId.equals(passengerId)) {
            throw new NotRideDriverException(rideId, userId);
        }

        BookingResponseDto dto = bookingMapper.toResponseDto(booking);
        return bookingResponseEnricher.enrich(booking, dto);
    }

    @Override
    @Transactional
    public BookingResponseDto confirmBooking(Long rideId, Long bookingId, Long driverId) {
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new NoSuchRideException(rideId));

        verifyDriver(ride, driverId);

        RideBooking booking = findBookingForRide(rideId, bookingId);
        guardTransition(booking, BookingStatus.CONFIRMED);

        int available = ride.getAvailableSeatsForSegment(
                booking.getBoardStop().getStopOrder(),
                booking.getAlightStop().getStopOrder());
        // Subtract this booking's own seats since it's still PENDING (active) and counted
        int availableExcludingSelf = available + booking.getSeatCount();
        if (availableExcludingSelf < booking.getSeatCount()) {
            throw new InsufficientSeatsException(rideId, booking.getSeatCount(), availableExcludingSelf);
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setResolvedAt(Instant.now());
        ride.setLastModified(Instant.now());

        eventPublisher.publishEvent(new BookingConfirmedEvent(
                booking.getId(), rideId, booking.getPassenger().getId(), driverId));

        BookingResponseDto dto = bookingMapper.toResponseDto(booking);
        return bookingResponseEnricher.enrich(booking, dto);
    }

    @Override
    @Transactional
    public BookingResponseDto rejectBooking(Long rideId, Long bookingId, Long driverId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NoSuchRideException(rideId));

        verifyDriver(ride, driverId);

        RideBooking booking = findBookingForRide(rideId, bookingId);
        guardTransition(booking, BookingStatus.REJECTED);

        booking.setStatus(BookingStatus.REJECTED);
        booking.setResolvedAt(Instant.now());
        ride.setLastModified(Instant.now());

        eventPublisher.publishEvent(new BookingRejectedEvent(
                booking.getId(), rideId, booking.getPassenger().getId(), driverId));

        BookingResponseDto dto = bookingMapper.toResponseDto(booking);
        return bookingResponseEnricher.enrich(booking, dto);
    }

    @Override
    @Transactional
    public BookingResponseDto cancelBooking(Long rideId, Long bookingId, Long userId, String reason) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NoSuchRideException(rideId));

        RideBooking booking = findBookingForRide(rideId, bookingId);

        // Idempotent: if already in a terminal cancellation status, return current state
        if (booking.getStatus() == BookingStatus.CANCELLED_BY_DRIVER
                || booking.getStatus() == BookingStatus.CANCELLED_BY_PASSENGER) {
            BookingResponseDto dto = bookingMapper.toResponseDto(booking);
            return bookingResponseEnricher.enrich(booking, dto);
        }

        // Reason is required only for confirmed bookings
        if (booking.getStatus() == BookingStatus.CONFIRMED
                && (reason == null || reason.isBlank())) {
            throw new IllegalArgumentException("Cancellation reason is required for confirmed bookings");
        }

        Long driverId = ride.getDriver().getId();
        Long passengerId = booking.getPassenger().getId();

        BookingStatus targetStatus;
        if (userId.equals(driverId)) {
            targetStatus = BookingStatus.CANCELLED_BY_DRIVER;
        } else if (userId.equals(passengerId)) {
            targetStatus = BookingStatus.CANCELLED_BY_PASSENGER;
        } else {
            throw new NotRideDriverException(rideId, userId);
        }

        guardTransition(booking, targetStatus);

        booking.setStatus(targetStatus);
        booking.setResolvedAt(Instant.now());
        if (reason != null && !reason.isBlank()) {
            booking.setCancellationReason(reason);
        }
        booking.setCancelledAt(Instant.now());
        ride.setLastModified(Instant.now());

        eventPublisher.publishEvent(new BookingCancelledEvent(
                booking.getId(), rideId, passengerId, driverId, userId, reason));

        BookingResponseDto dto = bookingMapper.toResponseDto(booking);
        return bookingResponseEnricher.enrich(booking, dto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getBookingsForRide(Long rideId, Long driverId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NoSuchRideException(rideId));

        verifyDriver(ride, driverId);

        List<RideBooking> bookings = bookingRepository.findByRideId(rideId);
        List<BookingResponseDto> dtos = bookingMapper.toResponseDtos(bookings);
        return bookingResponseEnricher.enrich(bookings, dtos);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getMyBookings(Long passengerId) {
        if (!userAccountRepository.existsById(passengerId)) {
            throw new NoSuchUserException(passengerId);
        }

        List<RideBooking> bookings = bookingRepository.findByPassengerIdAndStatusIn(
                passengerId, ACTIVE_STATUSES);
        List<BookingResponseDto> dtos = bookingMapper.toResponseDtos(bookings);
        dtos = bookingResponseEnricher.enrich(bookings, dtos);
        return bookingResponseEnricher.enrichForPassenger(bookings, dtos);
    }

    private RideBooking findBookingForRide(Long rideId, Long bookingId) {
        RideBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(rideId, bookingId));
        if (!booking.getRide().getId().equals(rideId)) {
            throw new BookingNotFoundException(rideId, bookingId);
        }
        return booking;
    }

    private void verifyDriver(Ride ride, Long userId) {
        if (!ride.getDriver().getId().equals(userId)) {
            throw new NotRideDriverException(ride.getId(), userId);
        }
    }

    private void guardTransition(RideBooking booking, BookingStatus target) {
        if (!booking.getStatus().canTransitionTo(target)) {
            throw new InvalidBookingTransitionException(booking.getId(), booking.getStatus(), target);
        }
    }

    private RideStop findStop(Ride ride, Long osmId) {
        return ride.getStops().stream()
                .filter(s -> s.getLocation().getOsmId().equals(osmId))
                .findFirst()
                .orElseThrow(() -> new InvalidBookingSegmentException(
                        ride.getId(), "Stop with osmId " + osmId + " not found on this ride"));
    }
}
