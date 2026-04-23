package com.vamigo.match.internal;

import com.vamigo.location.Location;
import com.vamigo.ride.Ride;
import com.vamigo.ride.RideStop;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.domain.Specification;

/**
 * Stop-aware proximity specifications for {@link Ride}. A ride is a
 * match if <em>some</em> stop is near the origin and <em>some later</em>
 * stop is near the destination. Ordering is by the sum of the nearest
 * origin- and destination-stop distances.
 *
 * <p>Folded here from the original {@code RideSpecifications} so that
 * all geo predicates share {@link GeoMatchPredicates} and SRID / unit
 * concerns live in one place.
 */
public final class RideStopAwareSpecs {

    private RideStopAwareSpecs() {
    }

    public static Specification<Ride> hasStopNearOrigin(Double lat, Double lon, double radiusMeters) {
        return (root, query, cb) -> {
            if (lat == null || lon == null) return null;

            Subquery<Long> stopExists = query.subquery(Long.class);
            Root<RideStop> stopRoot = stopExists.from(RideStop.class);
            Join<RideStop, Location> locJoin = stopRoot.join("location");

            Subquery<Long> hasLaterStop = query.subquery(Long.class);
            Root<RideStop> laterRoot = hasLaterStop.from(RideStop.class);
            hasLaterStop.select(cb.literal(1L))
                    .where(
                            cb.equal(laterRoot.get("ride"), root),
                            cb.greaterThan(laterRoot.get("stopOrder"), stopRoot.get("stopOrder"))
                    );

            stopExists.select(cb.literal(1L))
                    .where(
                            cb.equal(stopRoot.get("ride"), root),
                            GeoMatchPredicates.withinRadius(cb, locJoin.get("coordinates"), lon, lat, radiusMeters),
                            cb.exists(hasLaterStop)
                    );

            return cb.exists(stopExists);
        };
    }

    public static Specification<Ride> hasStopNearDestination(Double lat, Double lon, double radiusMeters) {
        return (root, query, cb) -> {
            if (lat == null || lon == null) return null;

            Subquery<Long> stopExists = query.subquery(Long.class);
            Root<RideStop> stopRoot = stopExists.from(RideStop.class);
            Join<RideStop, Location> locJoin = stopRoot.join("location");

            Subquery<Long> hasEarlierStop = query.subquery(Long.class);
            Root<RideStop> earlierRoot = hasEarlierStop.from(RideStop.class);
            hasEarlierStop.select(cb.literal(1L))
                    .where(
                            cb.equal(earlierRoot.get("ride"), root),
                            cb.lessThan(earlierRoot.get("stopOrder"), stopRoot.get("stopOrder"))
                    );

            stopExists.select(cb.literal(1L))
                    .where(
                            cb.equal(stopRoot.get("ride"), root),
                            GeoMatchPredicates.withinRadius(cb, locJoin.get("coordinates"), lon, lat, radiusMeters),
                            cb.exists(hasEarlierStop)
                    );

            return cb.exists(stopExists);
        };
    }

    public static Specification<Ride> orderByNearestStopDistance(
            double originLat, double originLon,
            double destLat, double destLon,
            double radiusMeters) {
        return (root, query, cb) -> {
            Subquery<Double> minOriginDist = query.subquery(Double.class);
            Root<RideStop> s1 = minOriginDist.from(RideStop.class);
            Join<RideStop, Location> l1 = s1.join("location");
            Expression<Double> originDist = GeoMatchPredicates.distanceMeters(
                    cb, l1.get("coordinates"), originLon, originLat);
            minOriginDist.select(cb.min(originDist))
                    .where(
                            cb.equal(s1.get("ride"), root),
                            GeoMatchPredicates.withinRadius(cb, l1.get("coordinates"), originLon, originLat, radiusMeters)
                    );

            Subquery<Double> minDestDist = query.subquery(Double.class);
            Root<RideStop> s2 = minDestDist.from(RideStop.class);
            Join<RideStop, Location> l2 = s2.join("location");
            Expression<Double> destDistExpr = GeoMatchPredicates.distanceMeters(
                    cb, l2.get("coordinates"), destLon, destLat);
            minDestDist.select(cb.min(destDistExpr))
                    .where(
                            cb.equal(s2.get("ride"), root),
                            GeoMatchPredicates.withinRadius(cb, l2.get("coordinates"), destLon, destLat, radiusMeters)
                    );

            query.orderBy(
                    cb.asc(GeoMatchPredicates.combinedScore(cb, minOriginDist, minDestDist)),
                    cb.asc(root.get("id"))
            );
            return null;
        };
    }
}
