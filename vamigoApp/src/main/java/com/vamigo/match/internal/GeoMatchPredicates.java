package com.vamigo.match.internal;

import com.vamigo.domain.SpatialSpecifications;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.locationtech.jts.geom.Point;

/**
 * Single source of truth for the PostGIS math used by the matching layer.
 *
 * <p>All call sites (forward specs and inverse native SQL facade) build
 * predicates and distance expressions here so that SRID (4326), geography
 * cast, meter units, sum-of-distances scoring, and the exact-match preference
 * are consistent.
 *
 * <p>{@link com.vamigo.location.Location#getCoordinates()} is already
 * {@code geography(Point, 4326)} — no extra cast is needed on the entity
 * side. Literal points from the query are wrapped into a 4326 geography
 * once inside {@link SpatialSpecifications}.
 */
public final class GeoMatchPredicates {

    private GeoMatchPredicates() {
    }

    public static Predicate withinRadius(
            CriteriaBuilder cb, Expression<Point> coordinates,
            double lon, double lat, double radiusMeters) {
        return SpatialSpecifications.withinRadius(cb, coordinates, lon, lat, radiusMeters);
    }

    public static Expression<Double> distanceMeters(
            CriteriaBuilder cb, Expression<Point> coordinates,
            double lon, double lat) {
        return SpatialSpecifications.stDistance(cb, coordinates, lon, lat);
    }

    /**
     * Sum-of-distances score. {@code null} subqueries fall back to a very
     * large value so rows without a valid stop are pushed to the end of
     * the result set. Kept byte-identical to the original specification.
     */
    public static Expression<Double> combinedScore(
            CriteriaBuilder cb,
            Expression<Double> originDistance,
            Expression<Double> destinationDistance) {
        return cb.sum(
                cb.coalesce(originDistance, 99999999.9),
                cb.coalesce(destinationDistance, 99999999.9)
        );
    }
}
