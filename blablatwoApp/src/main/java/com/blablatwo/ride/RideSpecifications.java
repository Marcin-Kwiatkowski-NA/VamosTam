package com.blablatwo.ride;

import com.blablatwo.domain.SpatialSpecifications;
import com.blablatwo.domain.Status;
import com.blablatwo.domain.TimePredicateHelper;
import com.blablatwo.location.Location;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.locationtech.jts.geom.Point;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;

public class RideSpecifications {

    private RideSpecifications() {
    }

    public static Specification<Ride> hasStatus(Status status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Ride> hasStopWithOriginOsmId(Long osmId) {
        return (root, query, cb) -> {
            if (osmId == null) return null;
            query.distinct(true);
            Join<Ride, RideStop> stopJoin = root.join("stops");

            Subquery<Integer> maxOrder = query.subquery(Integer.class);
            Root<RideStop> maxRoot = maxOrder.from(RideStop.class);
            maxOrder.select(cb.max(maxRoot.get("stopOrder")))
                    .where(cb.equal(maxRoot.get("ride"), root));

            return cb.and(
                    cb.equal(stopJoin.get("location").get("osmId"), osmId),
                    cb.lessThan(stopJoin.get("stopOrder"), maxOrder)
            );
        };
    }

    public static Specification<Ride> hasStopWithDestinationOsmId(Long osmId) {
        return (root, query, cb) -> {
            if (osmId == null) return null;
            query.distinct(true);
            Join<Ride, RideStop> stopJoin = root.join("stops");
            return cb.and(
                    cb.equal(stopJoin.get("location").get("osmId"), osmId),
                    cb.greaterThan(stopJoin.get("stopOrder"), 0)
            );
        };
    }

    public static Specification<Ride> originBeforeDestination(Long originOsmId, Long destinationOsmId) {
        return (root, query, cb) -> {
            if (originOsmId == null || destinationOsmId == null) return null;

            Subquery<Integer> originOrder = query.subquery(Integer.class);
            Root<RideStop> originRoot = originOrder.from(RideStop.class);
            originOrder.select(cb.min(originRoot.get("stopOrder")))
                    .where(
                            cb.equal(originRoot.get("ride"), root),
                            cb.equal(originRoot.get("location").get("osmId"), originOsmId)
                    );

            Subquery<Integer> destOrder = query.subquery(Integer.class);
            Root<RideStop> destRoot = destOrder.from(RideStop.class);
            destOrder.select(cb.max(destRoot.get("stopOrder")))
                    .where(
                            cb.equal(destRoot.get("ride"), root),
                            cb.equal(destRoot.get("location").get("osmId"), destinationOsmId)
                    );

            return cb.lessThan(originOrder, destOrder);
        };
    }

    public static Specification<Ride> hasTotalSeatsAtLeast(Integer minSeats) {
        return (root, query, cb) ->
                minSeats == null ? null :
                        cb.greaterThanOrEqualTo(root.get("totalSeats"), minSeats);
    }

    public static Specification<Ride> departsOnOrAfter(LocalDate date, LocalTime time) {
        return TimePredicateHelper.departsOnOrAfter(date, time);
    }

    public static Specification<Ride> departsOnOrBefore(LocalDate date, LocalTime time) {
        return TimePredicateHelper.departsOnOrBefore(date, time);
    }

    public static Specification<Ride> hasStopNearOrigin(Double lat, Double lon, double radiusMeters) {
        return (root, query, cb) -> {
            if (lat == null || lon == null) return null;
            query.distinct(true);
            Join<Ride, RideStop> stopJoin = root.join("stops");
            Join<RideStop, Location> locJoin = stopJoin.join("location");

            Subquery<Long> hasLaterStop = query.subquery(Long.class);
            Root<RideStop> laterRoot = hasLaterStop.from(RideStop.class);
            hasLaterStop.select(cb.literal(1L))
                    .where(
                            cb.equal(laterRoot.get("ride"), root),
                            cb.greaterThan(laterRoot.get("stopOrder"), stopJoin.get("stopOrder"))
                    );

            return cb.and(
                    SpatialSpecifications.withinRadius(cb, locJoin.get("coordinates"), lon, lat, radiusMeters),
                    cb.exists(hasLaterStop)
            );
        };
    }

    public static Specification<Ride> hasStopNearDestination(Double lat, Double lon, double radiusMeters) {
        return (root, query, cb) -> {
            if (lat == null || lon == null) return null;
            query.distinct(true);
            Join<Ride, RideStop> stopJoin = root.join("stops");
            Join<RideStop, Location> locJoin = stopJoin.join("location");

            Subquery<Long> hasEarlierStop = query.subquery(Long.class);
            Root<RideStop> earlierRoot = hasEarlierStop.from(RideStop.class);
            hasEarlierStop.select(cb.literal(1L))
                    .where(
                            cb.equal(earlierRoot.get("ride"), root),
                            cb.lessThan(earlierRoot.get("stopOrder"), stopJoin.get("stopOrder"))
                    );

            return cb.and(
                    SpatialSpecifications.withinRadius(cb, locJoin.get("coordinates"), lon, lat, radiusMeters),
                    cb.exists(hasEarlierStop)
            );
        };
    }

    public static Specification<Ride> orderByNearestStopDistance(
            double originLat, double originLon,
            double destLat, double destLon,
            double radiusMeters) {
        return (root, query, cb) -> {
            @SuppressWarnings("unchecked")
            Expression<Point> originPoint = (Expression<Point>) (Expression<?>) cb.function("st_setsrid",
                    Object.class,
                    cb.function("st_makepoint", Object.class, cb.literal(originLon), cb.literal(originLat)),
                    cb.literal(4326));

            @SuppressWarnings("unchecked")
            Expression<Point> destPoint = (Expression<Point>) (Expression<?>) cb.function("st_setsrid",
                    Object.class,
                    cb.function("st_makepoint", Object.class, cb.literal(destLon), cb.literal(destLat)),
                    cb.literal(4326));

            Subquery<Double> minOriginDist = query.subquery(Double.class);
            Root<RideStop> s1 = minOriginDist.from(RideStop.class);
            Join<RideStop, Location> l1 = s1.join("location");
            minOriginDist.select(cb.min(
                    cb.function("st_distance", Double.class, l1.get("coordinates"), originPoint, cb.literal(true))
            )).where(
                    cb.equal(s1.get("ride"), root),
                    SpatialSpecifications.withinRadius(cb, l1.get("coordinates"), originLon, originLat, radiusMeters)
            );

            Subquery<Double> minDestDist = query.subquery(Double.class);
            Root<RideStop> s2 = minDestDist.from(RideStop.class);
            Join<RideStop, Location> l2 = s2.join("location");
            minDestDist.select(cb.min(
                    cb.function("st_distance", Double.class, l2.get("coordinates"), destPoint, cb.literal(true))
            )).where(
                    cb.equal(s2.get("ride"), root),
                    SpatialSpecifications.withinRadius(cb, l2.get("coordinates"), destLon, destLat, radiusMeters)
            );

            query.orderBy(cb.asc(
                    cb.sum(
                            cb.coalesce(minOriginDist, 99999999.9),
                            cb.coalesce(minDestDist, 99999999.9)
                    )
            ));

            return null;
        };
    }

}
