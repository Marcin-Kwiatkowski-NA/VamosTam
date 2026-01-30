package com.blablatwo.ride;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class RideSpecifications {

    private RideSpecifications() {
        // Utility class
    }

    public static Specification<Ride> hasStatus(RideStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("rideStatus"), status);
    }

    /**
     * Filter by origin city's placeId (exact match).
     */
    public static Specification<Ride> originPlaceIdEquals(Long placeId) {
        return (root, query, cb) ->
                placeId == null ? null :
                        cb.equal(root.get("origin").get("placeId"), placeId);
    }

    /**
     * Filter by destination city's placeId (exact match).
     */
    public static Specification<Ride> destinationPlaceIdEquals(Long placeId) {
        return (root, query, cb) ->
                placeId == null ? null :
                        cb.equal(root.get("destination").get("placeId"), placeId);
    }

    public static Specification<Ride> departureAfter(LocalDateTime from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("departureTime"), from);
    }

    public static Specification<Ride> departureBefore(LocalDateTime to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThanOrEqualTo(root.get("departureTime"), to);
    }

    public static Specification<Ride> hasMinAvailableSeats(Integer minSeats) {
        return (root, query, cb) ->
                minSeats == null ? null : cb.greaterThanOrEqualTo(root.get("availableSeats"), minSeats);
    }
}
