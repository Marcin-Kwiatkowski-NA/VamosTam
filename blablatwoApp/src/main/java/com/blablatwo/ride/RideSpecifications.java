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

    public static Specification<Ride> originNameContains(String originName) {
        return (root, query, cb) ->
                originName == null || originName.isBlank() ? null :
                        cb.like(cb.lower(root.get("origin").get("name")),
                                "%" + originName.toLowerCase() + "%");
    }

    public static Specification<Ride> destinationNameContains(String destinationName) {
        return (root, query, cb) ->
                destinationName == null || destinationName.isBlank() ? null :
                        cb.like(cb.lower(root.get("destination").get("name")),
                                "%" + destinationName.toLowerCase() + "%");
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
