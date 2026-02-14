package com.blablatwo.ride;

import com.blablatwo.domain.Status;
import com.blablatwo.domain.TimePredicateHelper;
import com.blablatwo.exceptions.AlreadyBookedException;
import com.blablatwo.exceptions.BookingNotFoundException;
import com.blablatwo.exceptions.CannotBookException;
import com.blablatwo.exceptions.CannotCreateRideException;
import com.blablatwo.exceptions.ExternalRideNotBookableException;
import com.blablatwo.exceptions.InvalidBookingSegmentException;
import com.blablatwo.exceptions.NoSuchRideException;
import com.blablatwo.exceptions.RideHasBookingsException;
import com.blablatwo.exceptions.RideNotBookableException;
import com.blablatwo.exceptions.SegmentFullException;
import com.blablatwo.location.Location;
import com.blablatwo.location.LocationResolutionService;
import com.blablatwo.ride.dto.BookRideRequest;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.ride.dto.RideSearchCriteriaDto;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.capability.CapabilityService;
import com.blablatwo.user.exception.NoSuchUserException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RideServiceImpl implements RideService {

    private final RideRepository rideRepository;
    private final RideBookingRepository bookingRepository;
    private final RideMapper rideMapper;
    private final LocationResolutionService locationResolutionService;
    private final UserAccountRepository userAccountRepository;
    private final RideResponseEnricher rideResponseEnricher;
    private final CapabilityService capabilityService;

    public RideServiceImpl(RideRepository rideRepository,
                           RideBookingRepository bookingRepository,
                           RideMapper rideMapper,
                           LocationResolutionService locationResolutionService,
                           UserAccountRepository userAccountRepository,
                           RideResponseEnricher rideResponseEnricher,
                           CapabilityService capabilityService) {
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
        this.rideMapper = rideMapper;
        this.locationResolutionService = locationResolutionService;
        this.userAccountRepository = userAccountRepository;
        this.rideResponseEnricher = rideResponseEnricher;
        this.capabilityService = capabilityService;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RideResponseDto> getById(Long id) {
        return rideRepository.findById(id)
                .map(ride -> rideResponseEnricher.enrich(ride, rideMapper.rideEntityToRideResponseDto(ride)));
    }

    @Override
    @Transactional
    public RideResponseDto createForCurrentUser(RideCreationDto dto, Long userId) {
        if (!capabilityService.canCreateRide(userId)) {
            throw new CannotCreateRideException(userId);
        }

        UserAccount driver = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        var newRide = rideMapper.rideCreationDtoToEntity(dto);
        newRide.setDriver(driver);
        newRide.setLastModified(Instant.now());

        List<RideStop> stops = buildStops(newRide, dto);
        newRide.setStops(stops);
        setDenormalizedDepartureFields(newRide, dto.departureTime());
        newRide.setApproximate(dto.isApproximate());

        Ride saved = rideRepository.save(newRide);
        return rideResponseEnricher.enrich(saved, rideMapper.rideEntityToRideResponseDto(saved));
    }

    @Override
    @Transactional
    public RideResponseDto update(RideCreationDto dto, Long id) {
        var existingRide = rideRepository.findById(id)
                .orElseThrow(() -> new NoSuchRideException(id));

        if (existingRide.getBookings() != null && !existingRide.getBookings().isEmpty()) {
            throw new RideHasBookingsException(id);
        }

        rideMapper.update(existingRide, dto);
        updateStops(existingRide, dto);
        setDenormalizedDepartureFields(existingRide, dto.departureTime());
        existingRide.setApproximate(dto.isApproximate());
        existingRide.setLastModified(Instant.now());

        return rideResponseEnricher.enrich(existingRide, rideMapper.rideEntityToRideResponseDto(existingRide));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (rideRepository.existsById(id)) {
            rideRepository.deleteById(id);
        } else {
            throw new NoSuchRideException(id);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RideResponseDto> searchRides(RideSearchCriteriaDto criteria, Pageable pageable) {
        var departureFrom = TimePredicateHelper.calculateDepartureFrom(
                criteria.departureDate(), criteria.departureTimeFrom());

        Specification<Ride> spec = Specification.where(RideSpecifications.hasStatus(Status.ACTIVE))
                .and(RideSpecifications.hasStopWithOriginOsmId(criteria.originOsmId()))
                .and(RideSpecifications.hasStopWithDestinationOsmId(criteria.destinationOsmId()))
                .and(RideSpecifications.originBeforeDestination(criteria.originOsmId(), criteria.destinationOsmId()))
                .and(RideSpecifications.hasTotalSeatsAtLeast(criteria.minAvailableSeats()))
                .and(RideSpecifications.departsOnOrAfter(departureFrom.date(), departureFrom.time()));

        if (criteria.departureDateTo() != null) {
            spec = spec.and(RideSpecifications.departsOnOrBefore(
                    criteria.departureDateTo(), LocalTime.MAX));
        }

        Page<Ride> ridePage = rideRepository.findAll(spec, pageable);

        List<Ride> filtered = ridePage.getContent().stream()
                .filter(ride -> hasAvailableSeatsForSearch(ride, criteria))
                .toList();

        List<RideResponseDto> dtos = filtered.stream()
                .map(rideMapper::rideEntityToRideResponseDto)
                .toList();
        List<RideResponseDto> enriched = rideResponseEnricher.enrich(filtered, dtos);
        return new PageImpl<>(enriched, pageable, ridePage.getTotalElements());
    }

    private boolean hasAvailableSeatsForSearch(Ride ride, RideSearchCriteriaDto criteria) {
        if (criteria.originOsmId() == null || criteria.destinationOsmId() == null) {
            return true;
        }
        int boardOrder = findStopOrder(ride, criteria.originOsmId());
        int alightOrder = findStopOrder(ride, criteria.destinationOsmId());
        if (boardOrder < 0 || alightOrder < 0 || boardOrder >= alightOrder) return false;
        int minSeats = criteria.minAvailableSeats() > 0 ? criteria.minAvailableSeats() : 1;
        return ride.getAvailableSeatsForSegment(boardOrder, alightOrder) >= minSeats;
    }

    private int findStopOrder(Ride ride, Long osmId) {
        return ride.getStops().stream()
                .filter(s -> s.getLocation().getOsmId().equals(osmId))
                .findFirst()
                .map(RideStop::getStopOrder)
                .orElse(-1);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RideResponseDto> getAllRides(Pageable pageable) {
        Page<Ride> ridePage = rideRepository.findAll(pageable);
        List<Ride> rides = ridePage.getContent();
        List<RideResponseDto> dtos = rides.stream()
                .map(rideMapper::rideEntityToRideResponseDto)
                .toList();
        List<RideResponseDto> enriched = rideResponseEnricher.enrich(rides, dtos);
        return new PageImpl<>(enriched, pageable, ridePage.getTotalElements());
    }

    @Override
    @Transactional
    public RideResponseDto bookRide(Long rideId, Long passengerId, BookRideRequest request) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NoSuchRideException(rideId));

        if (ride.getSource() != RideSource.INTERNAL) {
            throw new ExternalRideNotBookableException(rideId);
        }

        if (!capabilityService.canBook(passengerId)) {
            throw new CannotBookException(passengerId);
        }

        if (bookingRepository.existsByRideIdAndPassengerId(rideId, passengerId)) {
            throw new AlreadyBookedException(rideId, passengerId);
        }

        UserAccount passenger = userAccountRepository.findById(passengerId)
                .orElseThrow(() -> new NoSuchUserException(passengerId));

        if (ride.computeRideStatus() != RideStatus.OPEN) {
            throw new RideNotBookableException(rideId, ride.computeRideStatus().name());
        }

        RideStop boardStop = findStop(ride, request.boardStopOsmId());
        RideStop alightStop = findStop(ride, request.alightStopOsmId());

        if (boardStop.getStopOrder() >= alightStop.getStopOrder()) {
            throw new InvalidBookingSegmentException(rideId,
                    "Board stop must come before alight stop");
        }

        int available = ride.getAvailableSeatsForSegment(
                boardStop.getStopOrder(), alightStop.getStopOrder());
        if (available <= 0) {
            throw new SegmentFullException(rideId,
                    boardStop.getStopOrder(), alightStop.getStopOrder());
        }

        RideBooking booking = RideBooking.builder()
                .ride(ride)
                .passenger(passenger)
                .boardStop(boardStop)
                .alightStop(alightStop)
                .bookedAt(Instant.now())
                .build();

        bookingRepository.save(booking);
        ride.setLastModified(Instant.now());
        rideRepository.save(ride);

        return rideResponseEnricher.enrich(ride, rideMapper.rideEntityToRideResponseDto(ride));
    }

    @Override
    @Transactional
    public RideResponseDto cancelBooking(Long rideId, Long passengerId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new NoSuchRideException(rideId));

        if (ride.getSource() != RideSource.INTERNAL) {
            throw new ExternalRideNotBookableException(rideId);
        }

        if (!userAccountRepository.existsById(passengerId)) {
            throw new NoSuchUserException(passengerId);
        }

        RideBooking booking = bookingRepository.findByRideIdAndPassengerId(rideId, passengerId)
                .orElseThrow(() -> new BookingNotFoundException(rideId, passengerId));

        bookingRepository.delete(booking);
        ride.setLastModified(Instant.now());
        rideRepository.save(ride);

        return rideResponseEnricher.enrich(ride, rideMapper.rideEntityToRideResponseDto(ride));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideResponseDto> getRidesForPassenger(Long passengerId) {
        if (!userAccountRepository.existsById(passengerId)) {
            throw new NoSuchUserException(passengerId);
        }

        List<RideBooking> bookings = bookingRepository.findByPassengerId(passengerId);
        List<Ride> rides = bookings.stream()
                .map(RideBooking::getRide)
                .distinct()
                .toList();
        List<RideResponseDto> dtos = rides.stream()
                .map(rideMapper::rideEntityToRideResponseDto)
                .toList();
        return rideResponseEnricher.enrich(rides, dtos);
    }

    private List<RideStop> buildStops(Ride ride, RideCreationDto dto) {
        Location origin = locationResolutionService.resolve(dto.origin());
        Location destination = locationResolutionService.resolve(dto.destination());

        List<RideStop> stops = new ArrayList<>();
        int order = 0;

        stops.add(RideStop.builder()
                .ride(ride).location(origin).stopOrder(order++)
                .departureTime(dto.departureTime()).build());

        if (dto.intermediateStops() != null) {
            for (var intermediateStop : dto.intermediateStops()) {
                Location loc = locationResolutionService.resolve(intermediateStop.location());
                stops.add(RideStop.builder()
                        .ride(ride).location(loc).stopOrder(order++)
                        .departureTime(intermediateStop.departureTime()).build());
            }
        }

        stops.add(RideStop.builder()
                .ride(ride).location(destination).stopOrder(order)
                .departureTime(null).build());

        validateStopTimeOrdering(stops);
        return stops;
    }

    private void updateStops(Ride existingRide, RideCreationDto dto) {
        List<Long> existingOsmIds = existingRide.getStops().stream()
                .map(s -> s.getLocation().getOsmId())
                .toList();

        List<Long> newOsmIds = buildNewOsmIdSequence(dto);

        if (existingOsmIds.equals(newOsmIds)) {
            existingRide.getStops().get(0).setDepartureTime(dto.departureTime());
            if (dto.intermediateStops() != null) {
                for (int i = 0; i < dto.intermediateStops().size(); i++) {
                    existingRide.getStops().get(i + 1)
                            .setDepartureTime(dto.intermediateStops().get(i).departureTime());
                }
            }
            validateStopTimeOrdering(existingRide.getStops());
        } else {
            existingRide.getStops().clear();
            existingRide.getStops().addAll(buildStops(existingRide, dto));
        }
    }

    private List<Long> buildNewOsmIdSequence(RideCreationDto dto) {
        List<Long> ids = new ArrayList<>();
        ids.add(dto.origin().osmId());
        if (dto.intermediateStops() != null) {
            dto.intermediateStops().forEach(s -> ids.add(s.location().osmId()));
        }
        ids.add(dto.destination().osmId());
        return ids;
    }

    private void validateStopTimeOrdering(List<RideStop> stops) {
        LocalDateTime prev = null;
        for (RideStop stop : stops) {
            if (stop.getDepartureTime() == null) continue;
            if (prev != null && !stop.getDepartureTime().isAfter(prev)) {
                throw new IllegalArgumentException(
                        "Stop departure times must be strictly increasing. Stop order "
                                + stop.getStopOrder() + " violates ordering.");
            }
            prev = stop.getDepartureTime();
        }
    }

    private void setDenormalizedDepartureFields(Ride ride, LocalDateTime departureTime) {
        ride.setDepartureDate(departureTime.toLocalDate());
        ride.setDepartureTime(departureTime.toLocalTime());
    }

    private RideStop findStop(Ride ride, Long osmId) {
        return ride.getStops().stream()
                .filter(s -> s.getLocation().getOsmId().equals(osmId))
                .findFirst()
                .orElseThrow(() -> new InvalidBookingSegmentException(
                        ride.getId(), "Stop with osmId " + osmId + " not found on this ride"));
    }
}
