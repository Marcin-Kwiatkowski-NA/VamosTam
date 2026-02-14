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
}
