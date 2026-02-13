package com.blablatwo.ride;

import com.blablatwo.domain.Status;
import com.blablatwo.domain.TimePredicateHelper;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
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

    /**
     * Find rides that have a stop with the given placeId that is NOT the last stop
     * (passengers need at least one leg after boarding).
     */
    public static Specification<Ride> hasStopWithOriginPlaceId(Long placeId) {
        return (root, query, cb) -> {
            if (placeId == null) return null;
            query.distinct(true);
            Join<Ride, RideStop> stopJoin = root.join("stops");

            Subquery<Integer> maxOrder = query.subquery(Integer.class);
            Root<RideStop> maxRoot = maxOrder.from(RideStop.class);
            maxOrder.select(cb.max(maxRoot.get("stopOrder")))
                    .where(cb.equal(maxRoot.get("ride"), root));

            return cb.and(
                    cb.equal(stopJoin.get("city").get("placeId"), placeId),
                    cb.lessThan(stopJoin.get("stopOrder"), maxOrder)
            );
        };
    }

    /**
     * Find rides that have a stop with the given placeId that is NOT the first stop
     * (passengers need at least one leg before alighting).
     */
    public static Specification<Ride> hasStopWithDestinationPlaceId(Long placeId) {
        return (root, query, cb) -> {
            if (placeId == null) return null;
            query.distinct(true);
            Join<Ride, RideStop> stopJoin = root.join("stops");
            return cb.and(
                    cb.equal(stopJoin.get("city").get("placeId"), placeId),
                    cb.greaterThan(stopJoin.get("stopOrder"), 0)
            );
        };
    }

    /**
     * Ensure the origin stop comes before the destination stop in the route.
     */
    public static Specification<Ride> originBeforeDestination(Long originPlaceId, Long destinationPlaceId) {
        return (root, query, cb) -> {
            if (originPlaceId == null || destinationPlaceId == null) return null;

            Subquery<Integer> originOrder = query.subquery(Integer.class);
            Root<RideStop> originRoot = originOrder.from(RideStop.class);
            originOrder.select(cb.min(originRoot.get("stopOrder")))
                    .where(
                            cb.equal(originRoot.get("ride"), root),
                            cb.equal(originRoot.get("city").get("placeId"), originPlaceId)
                    );

            Subquery<Integer> destOrder = query.subquery(Integer.class);
            Root<RideStop> destRoot = destOrder.from(RideStop.class);
            destOrder.select(cb.max(destRoot.get("stopOrder")))
                    .where(
                            cb.equal(destRoot.get("ride"), root),
                            cb.equal(destRoot.get("city").get("placeId"), destinationPlaceId)
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
}
