package com.vamigo.ride;

import com.vamigo.exceptions.AlreadyBookedException;
import com.vamigo.exceptions.BookingNotFoundException;
import com.vamigo.exceptions.CannotBookException;
import com.vamigo.exceptions.CarrierRideNotBookableException;
import com.vamigo.exceptions.NoSuchRideException;
import com.vamigo.exceptions.NotRideDriverException;
import com.vamigo.exceptions.RideDepartedException;
import com.vamigo.ride.dto.BookRideRequest;
import com.vamigo.ride.dto.BookingResponseDto;
import com.vamigo.ride.event.BookingCancelledEvent;
import com.vamigo.ride.event.BookingConfirmedEvent;
import com.vamigo.ride.event.BookingRejectedEvent;
import com.vamigo.ride.event.BookingRequestedEvent;
import com.vamigo.user.AccountType;
import com.vamigo.user.CarrierProfile;
import com.vamigo.user.CarrierProfileRepository;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.UserProfileRepository;
import com.vamigo.user.capability.CapabilityService;
import com.vamigo.user.exception.NoSuchUserException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class BookingServiceImpl implements BookingService {

    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final RideRepository rideRepository;
    private final RideBookingRepository bookingRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final CarrierProfileRepository carrierProfileRepository;
    private final BookingMapper bookingMapper;
    private final BookingResponseEnricher bookingResponseEnricher;
    private final CapabilityService capabilityService;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    public BookingServiceImpl(RideRepository rideRepository,
                               RideBookingRepository bookingRepository,
                               UserAccountRepository userAccountRepository,
                               UserProfileRepository userProfileRepository,
                               CarrierProfileRepository carrierProfileRepository,
                               BookingMapper bookingMapper,
                               BookingResponseEnricher bookingResponseEnricher,
                               CapabilityService capabilityService,
                               ApplicationEventPublisher eventPublisher,
                               Clock clock) {
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
        this.userAccountRepository = userAccountRepository;
        this.userProfileRepository = userProfileRepository;
        this.carrierProfileRepository = carrierProfileRepository;
        this.bookingMapper = bookingMapper;
        this.bookingResponseEnricher = bookingResponseEnricher;
        this.capabilityService = capabilityService;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BookingResponseDto createBooking(Long rideId, Long passengerId, BookRideRequest request) {
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new NoSuchRideException(rideId));

        userProfileRepository.findById(ride.getDriver().getId())
                .filter(p -> p.getAccountType() == AccountType.CARRIER)
                .ifPresent(p -> {
                    boolean bookingEnabled = carrierProfileRepository.findById(p.getId())
                            .map(CarrierProfile::isBookingEnabled)
                            .orElse(false);
                    if (!bookingEnabled) {
                        throw new CarrierRideNotBookableException(rideId);
                    }
                });

        if (!capabilityService.canBook(passengerId)) {
            throw new CannotBookException(passengerId);
        }

        if (bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(rideId, passengerId, ACTIVE_STATUSES)) {
            throw new AlreadyBookedException(rideId, passengerId);
        }

        UserAccount passenger = userAccountRepository.findById(passengerId)
                .orElseThrow(() -> new NoSuchUserException(passengerId));

        RideBooking booking = ride.addBooking(
                passenger,
                request.boardStopOsmId(),
                request.alightStopOsmId(),
                request.seatCount(),
                request.proposedPrice(),
                Instant.now(clock));

        bookingRepository.save(booking);

        Long driverId = ride.getDriver().getId();
        if (booking.getStatus() == BookingStatus.PENDING) {
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
        RideBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException(rideId, bookingId));
        if (!booking.getRide().getId().equals(rideId)) {
            throw new BookingNotFoundException(rideId, bookingId);
        }

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
        verifyBookingBelongsToRide(ride, rideId, bookingId);

        RideBooking booking = ride.confirmBooking(bookingId, Instant.now(clock));

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
        verifyBookingBelongsToRide(ride, rideId, bookingId);

        RideBooking booking = ride.rejectBooking(bookingId, Instant.now(clock));

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

        RideBooking booking = findBookingInRide(ride, rideId, bookingId);

        if (ride.getDepartureTime() != null && ride.getDepartureTime().isBefore(Instant.now(clock))) {
            throw new RideDepartedException(rideId);
        }

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

        Instant now = Instant.now(clock);
        if (userId.equals(driverId)) {
            ride.cancelBookingByDriver(bookingId, reason, now);
        } else if (userId.equals(passengerId)) {
            ride.cancelBookingByPassenger(bookingId, reason, now);
        } else {
            throw new NotRideDriverException(rideId, userId);
        }

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

    private RideBooking findBookingInRide(Ride ride, Long rideId, Long bookingId) {
        return ride.getBookings().stream()
                .filter(b -> bookingId.equals(b.getId()))
                .findFirst()
                .orElseThrow(() -> new BookingNotFoundException(rideId, bookingId));
    }

    private void verifyDriver(Ride ride, Long userId) {
        if (!ride.getDriver().getId().equals(userId)) {
            throw new NotRideDriverException(ride.getId(), userId);
        }
    }

    private void verifyBookingBelongsToRide(Ride ride, Long rideId, Long bookingId) {
        boolean exists = ride.getBookings().stream()
                .anyMatch(b -> bookingId.equals(b.getId()));
        if (!exists) {
            throw new BookingNotFoundException(rideId, bookingId);
        }
    }
}
