package com.blablatwo.exceptions;

public class InvalidBookingSegmentException extends RuntimeException {
    public InvalidBookingSegmentException(Long rideId, String reason) {
        super("Invalid booking segment for ride " + rideId + ": " + reason);
    }
}
