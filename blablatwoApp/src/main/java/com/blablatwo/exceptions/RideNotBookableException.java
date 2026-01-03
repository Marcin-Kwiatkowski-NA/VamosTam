package com.blablatwo.exceptions;

public class RideNotBookableException extends RuntimeException {
    public RideNotBookableException(Long rideId, String status) {
        super("Ride " + rideId + " cannot be booked. Current status: " + status);
    }
}
