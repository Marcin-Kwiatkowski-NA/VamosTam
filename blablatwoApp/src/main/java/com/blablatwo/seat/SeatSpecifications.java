package com.blablatwo.seat;

import com.blablatwo.domain.Status;
import com.blablatwo.domain.TimePredicateHelper;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;

public class SeatSpecifications {

    private SeatSpecifications() {
    }

    public static Specification<Seat> hasStatus(Status status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Seat> originPlaceIdEquals(Long placeId) {
        return (root, query, cb) ->
                placeId == null ? null :
                        cb.equal(root.get("segment").get("origin").get("placeId"), placeId);
    }

    public static Specification<Seat> destinationPlaceIdEquals(Long placeId) {
        return (root, query, cb) ->
                placeId == null ? null :
                        cb.equal(root.get("segment").get("destination").get("placeId"), placeId);
    }

    public static Specification<Seat> departsOnOrAfter(LocalDate date, LocalTime time) {
        return TimePredicateHelper.departsOnOrAfter(date, time);
    }

    public static Specification<Seat> departsOnOrBefore(LocalDate date, LocalTime time) {
        return TimePredicateHelper.departsOnOrBefore(date, time);
    }

    public static Specification<Seat> countAtMost(Integer maxCount) {
        return (root, query, cb) ->
                maxCount == null ? null : cb.lessThanOrEqualTo(root.get("count"), maxCount);
    }
}
