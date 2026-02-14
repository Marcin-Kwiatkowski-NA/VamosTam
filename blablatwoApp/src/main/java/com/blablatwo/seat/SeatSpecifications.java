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

    public static Specification<Seat> originOsmIdEquals(Long osmId) {
        return (root, query, cb) ->
                osmId == null ? null :
                        cb.equal(root.get("origin").get("osmId"), osmId);
    }

    public static Specification<Seat> destinationOsmIdEquals(Long osmId) {
        return (root, query, cb) ->
                osmId == null ? null :
                        cb.equal(root.get("destination").get("osmId"), osmId);
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
