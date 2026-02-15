package com.blablatwo.domain;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.locationtech.jts.geom.Point;

public final class SpatialSpecifications {

    private SpatialSpecifications() {
    }

    public static Predicate withinRadius(
            CriteriaBuilder cb, Expression<Point> coordinates,
            double lon, double lat, double radiusMeters) {
        return cb.isTrue(cb.function("st_dwithin", Boolean.class,
                coordinates,
                makePoint(cb, lon, lat),
                cb.literal(radiusMeters)
        ));
    }

    public static Expression<Double> stDistance(
            CriteriaBuilder cb, Expression<Point> coordinates,
            double lon, double lat) {
        return cb.function("st_distance", Double.class,
                coordinates, makePoint(cb, lon, lat), cb.literal(true));
    }

    @SuppressWarnings("unchecked")
    private static Expression<Point> makePoint(CriteriaBuilder cb, double lon, double lat) {
        return (Expression<Point>) (Expression<?>) cb.function("st_setsrid",
                Object.class,
                cb.function("st_makepoint", Object.class,
                        cb.literal(lon), cb.literal(lat)),
                cb.literal(4326));
    }
}
