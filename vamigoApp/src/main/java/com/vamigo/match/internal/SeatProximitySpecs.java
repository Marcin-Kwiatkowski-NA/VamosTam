package com.vamigo.match.internal;

import com.vamigo.location.Location;
import com.vamigo.seat.Seat;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

/**
 * Point-to-point proximity specifications for {@link Seat}. Seats have
 * a single origin and destination (no stops), so predicates compare
 * the seat's origin/destination directly.
 */
public final class SeatProximitySpecs {

    private SeatProximitySpecs() {
    }

    public static Specification<Seat> originWithinRadius(Double lat, Double lon, double radiusMeters) {
        return (root, query, cb) -> {
            if (lat == null || lon == null) return null;
            Join<Seat, Location> originJoin = root.join("origin");
            return GeoMatchPredicates.withinRadius(cb, originJoin.get("coordinates"), lon, lat, radiusMeters);
        };
    }

    public static Specification<Seat> destinationWithinRadius(Double lat, Double lon, double radiusMeters) {
        return (root, query, cb) -> {
            if (lat == null || lon == null) return null;
            Join<Seat, Location> destJoin = root.join("destination");
            return GeoMatchPredicates.withinRadius(cb, destJoin.get("coordinates"), lon, lat, radiusMeters);
        };
    }

    public static Specification<Seat> orderByCombinedDistance(
            double originLat, double originLon,
            double destLat, double destLon) {
        return (root, query, cb) -> {
            Join<Seat, Location> originJoin = root.join("origin");
            Join<Seat, Location> destJoin = root.join("destination");
            Expression<Double> originDist = GeoMatchPredicates.distanceMeters(
                    cb, originJoin.get("coordinates"), originLon, originLat);
            Expression<Double> destDist = GeoMatchPredicates.distanceMeters(
                    cb, destJoin.get("coordinates"), destLon, destLat);
            query.orderBy(
                    cb.asc(cb.sum(originDist, destDist)),
                    cb.asc(root.get("id"))
            );
            return null;
        };
    }
}
