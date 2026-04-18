package com.vamigo.seat;

import com.vamigo.domain.Status;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

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

    public static Specification<Seat> departsOnOrAfter(Instant from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("departureTime"), from);
    }

    public static Specification<Seat> departsBefore(Instant to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThan(root.get("departureTime"), to);
    }

    public static Specification<Seat> countAtMost(Integer maxCount) {
        return (root, query, cb) ->
                maxCount == null ? null : cb.lessThanOrEqualTo(root.get("count"), maxCount);
    }
}
