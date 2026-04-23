package com.vamigo.ride;

import com.vamigo.domain.Status;
import com.vamigo.exceptions.CannotCreateRideException;
import com.vamigo.exceptions.NoSuchRideException;
import com.vamigo.exceptions.NotRideDriverException;
import com.vamigo.exceptions.RideHasBookingsException;
import com.vamigo.location.Location;
import com.vamigo.location.LocationResolutionService;
import com.vamigo.match.DateWindow;
import com.vamigo.match.GeoPoint;
import com.vamigo.match.LocationMatchingService;
import com.vamigo.match.MatchFilters;
import com.vamigo.match.MatchProperties;
import com.vamigo.match.RadiusStrategy;
import com.vamigo.match.RideMatch;
import com.vamigo.match.RideMatchQuery;
import com.vamigo.match.RouteQuery;
import com.vamigo.ride.dto.RideCreationDto;
import com.vamigo.ride.dto.RideListDto;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.ride.dto.RideSearchCriteriaDto;
import com.vamigo.ride.event.RideCompletedEvent;
import com.vamigo.ride.event.RideCreatedEvent;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.capability.CapabilityService;
import com.vamigo.user.exception.NoSuchUserException;
import com.vamigo.utils.PageableUtils;
import com.vamigo.vehicle.Vehicle;
import com.vamigo.vehicle.VehicleRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@EnableConfigurationProperties(RideBusinessProperties.class)
public class RideServiceImpl implements RideService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RideServiceImpl.class);
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);
    private static final List<BookingStatus> ACTIVE_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED);

    private final RideRepository rideRepository;
    private final RideBookingRepository bookingRepository;
    private final RideMapper rideMapper;
    private final LocationResolutionService locationResolutionService;
    private final UserAccountRepository userAccountRepository;
    private final RideResponseEnricher rideResponseEnricher;
    private final CapabilityService capabilityService;
    private final ApplicationEventPublisher eventPublisher;
    private final RideArrivalEstimator arrivalEstimator;
    private final LocationMatchingService locationMatchingService;
    private final MatchProperties matchProperties;
    private final RideBusinessProperties rideProperties;
    private final VehicleRepository vehicleRepository;
    private final Clock clock;

    public RideServiceImpl(RideRepository rideRepository,
                           RideBookingRepository bookingRepository,
                           RideMapper rideMapper,
                           LocationResolutionService locationResolutionService,
                           UserAccountRepository userAccountRepository,
                           RideResponseEnricher rideResponseEnricher,
                           CapabilityService capabilityService,
                           ApplicationEventPublisher eventPublisher,
                           RideArrivalEstimator arrivalEstimator,
                           LocationMatchingService locationMatchingService,
                           MatchProperties matchProperties,
                           RideBusinessProperties rideProperties,
                           VehicleRepository vehicleRepository,
                           Clock clock) {
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
        this.rideMapper = rideMapper;
        this.locationResolutionService = locationResolutionService;
        this.userAccountRepository = userAccountRepository;
        this.rideResponseEnricher = rideResponseEnricher;
        this.capabilityService = capabilityService;
        this.eventPublisher = eventPublisher;
        this.arrivalEstimator = arrivalEstimator;
        this.locationMatchingService = locationMatchingService;
        this.matchProperties = matchProperties;
        this.rideProperties = rideProperties;
        this.vehicleRepository = vehicleRepository;
        this.clock = clock;
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
        validateMinDepartureNotice(dto.departureTime());

        UserAccount driver = userAccountRepository.findById(userId)
                .orElseThrow(() -> new NoSuchUserException(userId));

        Ride newRide = Ride.builder().build();
        newRide.assignDriver(driver);

        if (dto.vehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findByIdAndOwnerId(dto.vehicleId(), userId)
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found or not owned"));
            newRide.assignVehicle(vehicle);
        }

        newRide.updateDetails(rideMapper.rideCreationDtoToDetails(dto));

        List<RideStop> stops = buildStops(newRide, dto);
        newRide.replaceStops(stops);

        newRide.recomputeArrival(arrivalEstimator.estimate(newRide));

        Ride saved = rideRepository.save(newRide);
        RideResponseDto response = rideResponseEnricher.enrich(saved, rideMapper.rideEntityToRideResponseDto(saved));
        eventPublisher.publishEvent(new RideCreatedEvent(saved.getId(), userId, response));
        return response;
    }

    @Override
    @Transactional
    public RideResponseDto update(RideCreationDto dto, Long id, Long driverId) {
        var existingRide = rideRepository.findById(id)
                .orElseThrow(() -> new NoSuchRideException(id));

        if (!existingRide.getDriver().getId().equals(driverId)) {
            throw new NotRideDriverException(id, driverId);
        }

        if (!existingRide.getActiveBookings().isEmpty()) {
            throw new RideHasBookingsException(id);
        }
        validateMinDepartureNotice(dto.departureTime());

        existingRide.updateDetails(rideMapper.rideCreationDtoToDetails(dto));
        updateStops(existingRide, dto);

        if (dto.vehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findByIdAndOwnerId(dto.vehicleId(), driverId)
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found or not owned"));
            existingRide.assignVehicle(vehicle);
        } else {
            existingRide.clearVehicle();
        }

        existingRide.recomputeArrival(arrivalEstimator.estimate(existingRide));

        return rideResponseEnricher.enrich(existingRide, rideMapper.rideEntityToRideResponseDto(existingRide));
    }

    @Override
    @Transactional
    public void delete(Long id, Long driverId) {
        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new NoSuchRideException(id));

        if (!ride.getDriver().getId().equals(driverId)) {
            throw new NotRideDriverException(id, driverId);
        }

        rideRepository.delete(ride);
    }

    @Override
    @Transactional
    public void cancelRide(Long id, Long driverId) {
        Ride ride = rideRepository.findById(id)
                .orElseThrow(() -> new NoSuchRideException(id));

        if (!ride.getDriver().getId().equals(driverId)) {
            throw new NotRideDriverException(id, driverId);
        }

        // Idempotent: already cancelled
        if (ride.getStatus() == Status.CANCELLED) {
            return;
        }

        if (!ride.getActiveBookings().isEmpty()) {
            throw new RideHasBookingsException(id);
        }

        ride.cancel();
    }

    @Override
    @Transactional
    public RideResponseDto completeRide(Long rideId, Long driverId) {
        Ride ride = rideRepository.findByIdForUpdate(rideId)
                .orElseThrow(() -> new NoSuchRideException(rideId));

        if (!ride.getDriver().getId().equals(driverId)) {
            throw new NotRideDriverException(rideId, driverId);
        }

        // Idempotent: already completed
        if (ride.getStatus() == Status.COMPLETED) {
            return rideResponseEnricher.enrich(ride, rideMapper.rideEntityToRideResponseDto(ride));
        }

        if (ride.getStatus() != Status.ACTIVE) {
            throw new IllegalStateException("Ride " + rideId + " cannot be completed from status " + ride.getStatus());
        }

        Instant now = Instant.now(clock);
        if (!ride.getDepartureTime().isBefore(now)) {
            throw new IllegalStateException("Ride " + rideId + " has not departed yet");
        }

        List<RideBooking> confirmedBookings = ride.getConfirmedBookings();
        if (confirmedBookings.isEmpty()) {
            throw new IllegalStateException("Ride " + rideId + " has no confirmed bookings");
        }

        ride.markCompleted(now);

        List<Long> confirmedBookingIds = confirmedBookings.stream()
                .map(RideBooking::getId)
                .toList();

        eventPublisher.publishEvent(new RideCompletedEvent(rideId, driverId, confirmedBookingIds));

        return rideResponseEnricher.enrich(ride, rideMapper.rideEntityToRideResponseDto(ride));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RideListDto> searchRides(RideSearchCriteriaDto criteria, Pageable pageable) {
        if (criteria.isProximityMode()) {
            return searchRidesNearby(criteria, pageable);
        }
        return searchRidesExact(criteria, pageable);
    }

    private Page<RideListDto> searchRidesExact(RideSearchCriteriaDto criteria, Pageable pageable) {
        Pageable stablePageable = PageableUtils.withStableSort(pageable);
        Instant effectiveEarliest = clampToNow(criteria.earliestDeparture(), Instant.now(clock));

        Specification<Ride> spec = Specification.where(RideSpecifications.hasStatus(Status.ACTIVE))
                .and(RideSpecifications.hasDriverId(criteria.driverId()))
                .and(RideSpecifications.hasStopWithOriginOsmId(criteria.originOsmId()))
                .and(RideSpecifications.hasStopWithDestinationOsmId(criteria.destinationOsmId()))
                .and(RideSpecifications.originBeforeDestination(criteria.originOsmId(), criteria.destinationOsmId()))
                .and(RideSpecifications.hasTotalSeatsAtLeast(criteria.minAvailableSeats()))
                .and(RideSpecifications.departsOnOrAfter(effectiveEarliest));

        if (criteria.latestDeparture() != null) {
            spec = spec.and(RideSpecifications.departsBefore(criteria.latestDeparture()));
        }

        Page<Ride> ridePage = rideRepository.findAll(spec, stablePageable);

        List<Ride> filtered = ridePage.getContent().stream()
                .filter(ride -> hasAvailableSeatsForExactSearch(ride, criteria))
                .toList();

        List<RideListDto> dtos = filtered.stream()
                .map(rideMapper::rideEntityToRideListDto)
                .toList();
        List<RideListDto> enriched = rideResponseEnricher.enrichList(filtered, dtos);
        return new PageImpl<>(enriched, stablePageable, ridePage.getTotalElements());
    }

    private Page<RideListDto> searchRidesNearby(RideSearchCriteriaDto criteria, Pageable pageable) {
        Instant effectiveEarliest = clampToNow(criteria.earliestDeparture(), Instant.now(clock));
        RadiusStrategy radius = criteria.radiusKm() != null
                ? RadiusStrategy.fixedKm(criteria.radiusKm())
                : matchProperties.rideSmartRadius();

        var query = new RideMatchQuery(
                new RouteQuery(
                        new GeoPoint(criteria.originLat(), criteria.originLon()),
                        new GeoPoint(criteria.destinationLat(), criteria.destinationLon()),
                        criteria.originOsmId(), criteria.destinationOsmId()),
                radius,
                DateWindow.of(effectiveEarliest, criteria.latestDeparture()),
                new MatchFilters(criteria.driverId(), null, criteria.minAvailableSeats())
        );

        Page<RideMatch> matchPage = locationMatchingService.findRides(query, pageable);

        List<Ride> filtered = matchPage.getContent().stream()
                .map(RideMatch::ride)
                .filter(ride -> hasAvailableSeatsForNearbySearch(ride, criteria))
                .toList();

        LOGGER.info("Proximity ride search: found={}, filtered={}",
                matchPage.getTotalElements(), filtered.size());

        List<RideListDto> dtos = filtered.stream()
                .map(rideMapper::rideEntityToRideListDto)
                .toList();
        List<RideListDto> enriched = rideResponseEnricher.enrichList(filtered, dtos);
        return new PageImpl<>(enriched, pageable, matchPage.getTotalElements());
    }

    private boolean hasAvailableSeatsForExactSearch(Ride ride, RideSearchCriteriaDto criteria) {
        if (criteria.originOsmId() == null || criteria.destinationOsmId() == null) {
            return true;
        }
        int boardOrder = findStopOrderByOsmId(ride, criteria.originOsmId());
        int alightOrder = findStopOrderByOsmId(ride, criteria.destinationOsmId());
        if (boardOrder < 0 || alightOrder < 0 || boardOrder >= alightOrder) return false;
        int minSeats = criteria.minAvailableSeats() > 0 ? criteria.minAvailableSeats() : 1;
        return ride.getAvailableSeatsForSegment(boardOrder, alightOrder) >= minSeats;
    }

    private boolean hasAvailableSeatsForNearbySearch(Ride ride, RideSearchCriteriaDto criteria) {
        int boardOrder = findNearestStopOrder(ride, criteria.originLat(), criteria.originLon(), true);
        int alightOrder = findNearestStopOrder(ride, criteria.destinationLat(), criteria.destinationLon(), false);
        if (boardOrder < 0 || alightOrder < 0 || boardOrder >= alightOrder) return false;
        int minSeats = criteria.minAvailableSeats() > 0 ? criteria.minAvailableSeats() : 1;
        return ride.getAvailableSeatsForSegment(boardOrder, alightOrder) >= minSeats;
    }

    private int findNearestStopOrder(Ride ride, double lat, double lon, boolean excludeLast) {
        RideStop stop = findNearestStop(ride, lat, lon, excludeLast);
        return stop != null ? stop.getStopOrder() : -1;
    }

    private RideStop findNearestStop(Ride ride, double lat, double lon, boolean excludeLast) {
        Point searchPoint = GEOMETRY_FACTORY.createPoint(new Coordinate(lon, lat));
        int maxOrder = ride.getStops().stream().mapToInt(RideStop::getStopOrder).max().orElse(0);
        return ride.getStops().stream()
                .filter(s -> excludeLast ? s.getStopOrder() < maxOrder : s.getStopOrder() > 0)
                .min(Comparator.comparingDouble(s ->
                        s.getLocation().getCoordinates().distance(searchPoint)))
                .orElse(null);
    }

    private int findStopOrderByOsmId(Ride ride, Long osmId) {
        return ride.getStops().stream()
                .filter(s -> s.getLocation().getOsmId().equals(osmId))
                .findFirst()
                .map(RideStop::getStopOrder)
                .orElse(-1);
    }

    private static Instant clampToNow(Instant earliest, Instant now) {
        return earliest == null || earliest.isBefore(now) ? now : earliest;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RideListDto> getAllRides(Pageable pageable) {
        Pageable stablePageable = PageableUtils.withStableSort(pageable);
        Page<Ride> ridePage = rideRepository.findAll(stablePageable);
        List<Ride> rides = ridePage.getContent();
        List<RideListDto> dtos = rides.stream()
                .map(rideMapper::rideEntityToRideListDto)
                .toList();
        List<RideListDto> enriched = rideResponseEnricher.enrichList(rides, dtos);
        return new PageImpl<>(enriched, stablePageable, ridePage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideListDto> getRidesForDriver(Long driverId) {
        if (!userAccountRepository.existsById(driverId)) {
            throw new NoSuchUserException(driverId);
        }

        List<Ride> rides = rideRepository.findByDriverIdOrderByDepartureTimeAscIdAsc(driverId);
        List<RideListDto> dtos = rides.stream()
                .map(rideMapper::rideEntityToRideListDto)
                .toList();
        return rideResponseEnricher.enrichList(rides, dtos);
    }

    private List<RideStop> buildStops(Ride ride, RideCreationDto dto) {
        Location origin = locationResolutionService.resolve(dto.origin());
        Location destination = locationResolutionService.resolve(dto.destination());

        List<RideStop> stops = new ArrayList<>();
        int order = 0;

        stops.add(RideStop.builder()
                .ride(ride).location(origin).stopOrder(order++)
                .departureTime(dto.departureTime())
                .legPrice(dto.originLegPrice()).build());

        if (dto.intermediateStops() != null) {
            for (var intermediateStop : dto.intermediateStops()) {
                Location loc = locationResolutionService.resolve(intermediateStop.location());
                stops.add(RideStop.builder()
                        .ride(ride).location(loc).stopOrder(order++)
                        .departureTime(intermediateStop.departureTime())
                        .legPrice(intermediateStop.legPrice()).build());
            }
        }

        stops.add(RideStop.builder()
                .ride(ride).location(destination).stopOrder(order)
                .departureTime(null).build());

        validateStopTimeOrdering(stops);
        return stops;
    }

    private void updateStops(Ride existingRide, RideCreationDto dto) {
        List<RideStop> existingStops = existingRide.getStops();
        List<Long> existingOsmIds = existingStops.stream()
                .map(s -> s.getLocation().getOsmId())
                .toList();

        List<Long> newOsmIds = buildNewOsmIdSequence(dto);

        if (existingOsmIds.equals(newOsmIds)) {
            existingStops.get(0).updateLeg(dto.departureTime(), dto.originLegPrice());
            if (dto.intermediateStops() != null) {
                for (int i = 0; i < dto.intermediateStops().size(); i++) {
                    existingStops.get(i + 1).updateLeg(
                            dto.intermediateStops().get(i).departureTime(),
                            dto.intermediateStops().get(i).legPrice());
                }
            }
            validateStopTimeOrdering(existingStops);
        } else {
            existingRide.replaceStops(buildStops(existingRide, dto));
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

    private void validateMinDepartureNotice(Instant departureTime) {
        Instant earliest = Instant.now(clock).plus(rideProperties.minDepartureNoticeMinutes(), ChronoUnit.MINUTES);
        if (departureTime.isBefore(earliest)) {
            throw new IllegalArgumentException(
                    "Departure must be at least " + rideProperties.minDepartureNoticeMinutes() + " minutes from now");
        }
    }

    private void validateStopTimeOrdering(List<RideStop> stops) {
        Instant prev = null;
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
}
