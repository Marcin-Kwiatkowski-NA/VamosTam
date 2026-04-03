package com.vamigo.ride;

import com.vamigo.domain.Status;
import com.vamigo.domain.TimePrecision;
import com.vamigo.exceptions.CannotCreateRideException;
import com.vamigo.exceptions.NoSuchRideException;
import com.vamigo.exceptions.NotRideDriverException;
import com.vamigo.exceptions.RideHasBookingsException;
import com.vamigo.location.Location;
import com.vamigo.location.LocationResolutionService;
import com.vamigo.ride.dto.RideCreationDto;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.ride.dto.RideSearchCriteriaDto;
import com.vamigo.ride.event.RideCompletedEvent;
import com.vamigo.ride.event.RideCreatedEvent;
import com.vamigo.search.GeoUtils;
import com.vamigo.search.SearchProperties;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.capability.CapabilityService;
import com.vamigo.user.exception.NoSuchUserException;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@EnableConfigurationProperties({SearchProperties.class, RideBusinessProperties.class})
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
    private final SearchProperties searchProperties;
    private final RideBusinessProperties rideProperties;
    private final VehicleRepository vehicleRepository;

    public RideServiceImpl(RideRepository rideRepository,
                           RideBookingRepository bookingRepository,
                           RideMapper rideMapper,
                           LocationResolutionService locationResolutionService,
                           UserAccountRepository userAccountRepository,
                           RideResponseEnricher rideResponseEnricher,
                           CapabilityService capabilityService,
                           ApplicationEventPublisher eventPublisher,
                           RideArrivalEstimator arrivalEstimator,
                           SearchProperties searchProperties,
                           RideBusinessProperties rideProperties,
                           VehicleRepository vehicleRepository) {
        this.rideRepository = rideRepository;
        this.bookingRepository = bookingRepository;
        this.rideMapper = rideMapper;
        this.locationResolutionService = locationResolutionService;
        this.userAccountRepository = userAccountRepository;
        this.rideResponseEnricher = rideResponseEnricher;
        this.capabilityService = capabilityService;
        this.eventPublisher = eventPublisher;
        this.arrivalEstimator = arrivalEstimator;
        this.searchProperties = searchProperties;
        this.rideProperties = rideProperties;
        this.vehicleRepository = vehicleRepository;
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

        var newRide = rideMapper.rideCreationDtoToEntity(dto);
        newRide.setDriver(driver);
        newRide.setLastModified(Instant.now());

        if (dto.vehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findByIdAndOwnerId(dto.vehicleId(), userId)
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found or not owned"));
            newRide.setVehicle(vehicle);
        }

        List<RideStop> stops = buildStops(newRide, dto);
        newRide.setStops(stops);
        newRide.setDepartureTime(dto.departureTime());
        newRide.setTimePrecision(dto.timePrecision());
        if (dto.timePrecision() != TimePrecision.EXACT) {
            newRide.setAutoApprove(false);
        }

        newRide.setEstimatedArrivalAt(arrivalEstimator.estimate(newRide));

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

        rideMapper.update(existingRide, dto);
        updateStops(existingRide, dto);
        existingRide.setDepartureTime(dto.departureTime());
        existingRide.setTimePrecision(dto.timePrecision());
        if (dto.timePrecision() != TimePrecision.EXACT) {
            existingRide.setAutoApprove(false);
        }
        existingRide.setLastModified(Instant.now());
        existingRide.setEstimatedArrivalAt(arrivalEstimator.estimate(existingRide));

        if (dto.vehicleId() != null) {
            Vehicle vehicle = vehicleRepository.findByIdAndOwnerId(dto.vehicleId(), driverId)
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found or not owned"));
            existingRide.setVehicle(vehicle);
        } else {
            existingRide.setVehicle(null);
        }

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

        ride.setStatus(Status.CANCELLED);
        ride.setLastModified(Instant.now());
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

        if (!ride.getDepartureTime().isBefore(Instant.now())) {
            throw new IllegalStateException("Ride " + rideId + " has not departed yet");
        }

        List<RideBooking> confirmedBookings = ride.getConfirmedBookings();
        if (confirmedBookings.isEmpty()) {
            throw new IllegalStateException("Ride " + rideId + " has no confirmed bookings");
        }

        ride.setStatus(Status.COMPLETED);
        ride.setCompletedAt(Instant.now());
        ride.setLastModified(Instant.now());

        List<Long> confirmedBookingIds = confirmedBookings.stream()
                .map(RideBooking::getId)
                .toList();

        eventPublisher.publishEvent(new RideCompletedEvent(rideId, driverId, confirmedBookingIds));

        return rideResponseEnricher.enrich(ride, rideMapper.rideEntityToRideResponseDto(ride));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RideResponseDto> searchRides(RideSearchCriteriaDto criteria, Pageable pageable) {
        if (criteria.isProximityMode()) {
            return searchRidesNearby(criteria, pageable);
        }
        return searchRidesExact(criteria, pageable);
    }

    private Page<RideResponseDto> searchRidesExact(RideSearchCriteriaDto criteria, Pageable pageable) {
        Specification<Ride> spec = Specification.where(RideSpecifications.hasStatus(Status.ACTIVE))
                .and(RideSpecifications.hasStopWithOriginOsmId(criteria.originOsmId()))
                .and(RideSpecifications.hasStopWithDestinationOsmId(criteria.destinationOsmId()))
                .and(RideSpecifications.originBeforeDestination(criteria.originOsmId(), criteria.destinationOsmId()))
                .and(RideSpecifications.hasTotalSeatsAtLeast(criteria.minAvailableSeats()))
                .and(RideSpecifications.departsOnOrAfter(criteria.earliestDeparture()));

        if (criteria.latestDeparture() != null) {
            spec = spec.and(RideSpecifications.departsBefore(criteria.latestDeparture()));
        }

        Page<Ride> ridePage = rideRepository.findAll(spec, pageable);

        List<Ride> filtered = ridePage.getContent().stream()
                .filter(ride -> hasAvailableSeatsForExactSearch(ride, criteria))
                .toList();

        List<RideResponseDto> dtos = filtered.stream()
                .map(rideMapper::rideEntityToRideResponseDto)
                .toList();
        List<RideResponseDto> enriched = rideResponseEnricher.enrich(filtered, dtos);
        return new PageImpl<>(enriched, pageable, ridePage.getTotalElements());
    }

    private Page<RideResponseDto> searchRidesNearby(RideSearchCriteriaDto criteria, Pageable pageable) {
        double radiusKm = resolveRadiusKm(criteria);
        double radiusMeters = radiusKm * 1000;

        Specification<Ride> spec = Specification.where(RideSpecifications.hasStatus(Status.ACTIVE))
                .and(RideSpecifications.hasStopNearOrigin(criteria.originLat(), criteria.originLon(), radiusMeters))
                .and(RideSpecifications.hasStopNearDestination(criteria.destinationLat(), criteria.destinationLon(), radiusMeters))
                .and(RideSpecifications.hasTotalSeatsAtLeast(criteria.minAvailableSeats()))
                .and(RideSpecifications.departsOnOrAfter(criteria.earliestDeparture()))
                .and(RideSpecifications.orderByNearestStopDistance(
                        criteria.originLat(), criteria.originLon(),
                        criteria.destinationLat(), criteria.destinationLon(),
                        radiusMeters));

        if (criteria.latestDeparture() != null) {
            spec = spec.and(RideSpecifications.departsBefore(criteria.latestDeparture()));
        }

        // Strip pageable sort so the Specification's distance-based orderBy is not overwritten
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Ride> ridePage = rideRepository.findAll(spec, unsorted);

        List<Ride> filtered = ridePage.getContent().stream()
                .filter(ride -> hasAvailableSeatsForNearbySearch(ride, criteria))
                .toList();

        LOGGER.info("Proximity ride search: radiusKm={}, found={}, filtered={}",
                radiusKm, ridePage.getTotalElements(), filtered.size());

        List<RideResponseDto> dtos = filtered.stream()
                .map(rideMapper::rideEntityToRideResponseDto)
                .toList();
        List<RideResponseDto> enriched = rideResponseEnricher.enrich(filtered, dtos);
        return new PageImpl<>(enriched, pageable, ridePage.getTotalElements());
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

    private double resolveRadiusKm(RideSearchCriteriaDto criteria) {
        if (criteria.radiusKm() != null) {
            return criteria.radiusKm();
        }
        double distance = GeoUtils.haversineKm(
                criteria.originLat(), criteria.originLon(),
                criteria.destinationLat(), criteria.destinationLon());
        return Math.max(
                distance / searchProperties.proximity().radiusDivisor(),
                searchProperties.proximity().minRadiusKm());
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
    @Transactional(readOnly = true)
    public List<RideResponseDto> getRidesForDriver(Long driverId) {
        if (!userAccountRepository.existsById(driverId)) {
            throw new NoSuchUserException(driverId);
        }

        List<Ride> rides = rideRepository.findByDriverIdOrderByDepartureTimeAsc(driverId);
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
        List<Long> existingOsmIds = existingRide.getStops().stream()
                .map(s -> s.getLocation().getOsmId())
                .toList();

        List<Long> newOsmIds = buildNewOsmIdSequence(dto);

        if (existingOsmIds.equals(newOsmIds)) {
            existingRide.getStops().get(0).setDepartureTime(dto.departureTime());
            existingRide.getStops().get(0).setLegPrice(dto.originLegPrice());
            if (dto.intermediateStops() != null) {
                for (int i = 0; i < dto.intermediateStops().size(); i++) {
                    existingRide.getStops().get(i + 1)
                            .setDepartureTime(dto.intermediateStops().get(i).departureTime());
                    existingRide.getStops().get(i + 1)
                            .setLegPrice(dto.intermediateStops().get(i).legPrice());
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

    private void validateMinDepartureNotice(Instant departureTime) {
        Instant earliest = Instant.now().plus(rideProperties.minDepartureNoticeMinutes(), ChronoUnit.MINUTES);
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
