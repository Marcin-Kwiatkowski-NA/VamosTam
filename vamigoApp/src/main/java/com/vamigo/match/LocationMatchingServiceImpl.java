package com.vamigo.match;

import com.vamigo.domain.Status;
import com.vamigo.location.LocationDto;
import com.vamigo.match.internal.RideStopAwareSpecs;
import com.vamigo.match.internal.SavedSearchMatchingQuery;
import com.vamigo.match.internal.SeatProximitySpecs;
import com.vamigo.ride.Ride;
import com.vamigo.ride.RideRepository;
import com.vamigo.ride.RideSpecifications;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.seat.Seat;
import com.vamigo.seat.SeatRepository;
import com.vamigo.seat.SeatSpecifications;
import com.vamigo.seat.dto.SeatResponseDto;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@EnableConfigurationProperties(MatchProperties.class)
public class LocationMatchingServiceImpl implements LocationMatchingService {

    private final RideRepository rideRepository;
    private final SeatRepository seatRepository;
    private final SavedSearchMatchingQuery savedSearchMatchingQuery;
    private final MatchProperties matchProperties;

    public LocationMatchingServiceImpl(RideRepository rideRepository,
                                       SeatRepository seatRepository,
                                       SavedSearchMatchingQuery savedSearchMatchingQuery,
                                       MatchProperties matchProperties) {
        this.rideRepository = rideRepository;
        this.seatRepository = seatRepository;
        this.savedSearchMatchingQuery = savedSearchMatchingQuery;
        this.matchProperties = matchProperties;
    }

    // --- Forward ----------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public Page<RideMatch> findRides(RideMatchQuery q, Pageable pageable) {
        double radiusMeters = q.radius().resolveMeters(q.route().origin(), q.route().destination());
        GeoPoint origin = q.route().origin();
        GeoPoint dest = q.route().destination();

        Specification<Ride> spec = Specification.where(RideSpecifications.hasStatus(Status.ACTIVE))
                .and(RideSpecifications.hasDriverId(q.filters().driverIdFilter()))
                .and(RideStopAwareSpecs.hasStopNearOrigin(origin.lat(), origin.lon(), radiusMeters))
                .and(RideStopAwareSpecs.hasStopNearDestination(dest.lat(), dest.lon(), radiusMeters))
                .and(RideSpecifications.hasTotalSeatsAtLeast(q.filters().minAvailableSeats()))
                .and(RideSpecifications.departsOnOrAfter(q.window().earliest()))
                .and(RideStopAwareSpecs.orderByNearestStopDistance(
                        origin.lat(), origin.lon(), dest.lat(), dest.lon(), radiusMeters));

        if (q.window().latest() != null) {
            spec = spec.and(RideSpecifications.departsBefore(q.window().latest()));
        }

        // Strip pageable sort so the Specification's distance-based orderBy is not overwritten.
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Ride> ridePage = rideRepository.findAll(spec, unsorted);
        return ridePage.map(r -> new RideMatch(r, null));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<SeatMatch> findSeats(SeatMatchQuery q, Pageable pageable) {
        double radiusMeters = q.radius().resolveMeters(q.route().origin(), q.route().destination());
        GeoPoint origin = q.route().origin();
        GeoPoint dest = q.route().destination();

        Specification<Seat> spec = Specification.where(SeatSpecifications.hasStatus(Status.ACTIVE))
                .and(SeatProximitySpecs.originWithinRadius(origin.lat(), origin.lon(), radiusMeters))
                .and(SeatProximitySpecs.destinationWithinRadius(dest.lat(), dest.lon(), radiusMeters))
                .and(SeatSpecifications.countAtMost(q.filters().minAvailableSeats()))
                .and(SeatSpecifications.departsOnOrAfter(q.window().earliest()))
                .and(SeatProximitySpecs.orderByCombinedDistance(
                        origin.lat(), origin.lon(), dest.lat(), dest.lon()));

        if (q.window().latest() != null) {
            spec = spec.and(SeatSpecifications.departsBefore(q.window().latest()));
        }

        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Seat> seatPage = seatRepository.findAll(spec, unsorted);
        return seatPage.map(s -> new SeatMatch(s, null));
    }

    // --- Inverse ----------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<SavedSearchMatch> findSavedSearchesMatchingRide(RideResponseDto ride, Long driverId) {
        if (ride.stops() == null || ride.stops().size() < 2) return List.of();

        LocationDto originDto = ride.stops().get(0).location();
        LocationDto destDto = ride.stops().get(ride.stops().size() - 1).location();
        double radiusMeters = matchProperties.alertRadius().resolveMeters(
                new GeoPoint(originDto.latitude(), originDto.longitude()),
                new GeoPoint(destDto.latitude(), destDto.longitude())
        );
        return savedSearchMatchingQuery.matchRide(ride, driverId, radiusMeters);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedSearchMatch> findSavedSearchesMatchingSeat(SeatResponseDto seat, Long userId) {
        LocationDto originDto = seat.origin();
        LocationDto destDto = seat.destination();
        if (originDto == null || destDto == null) return List.of();

        double radiusMeters = matchProperties.alertRadius().resolveMeters(
                new GeoPoint(originDto.latitude(), originDto.longitude()),
                new GeoPoint(destDto.latitude(), destDto.longitude())
        );
        return savedSearchMatchingQuery.matchSeat(seat, userId, radiusMeters);
    }
}
