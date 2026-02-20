package com.blablatwo.exceptions;

public class BookingNotFoundException extends RuntimeException {

    public BookingNotFoundException(Long rideId, Long identifier) {
        super("Booking not found for ride " + rideId + " (identifier: " + identifier + ")");
    }
}
