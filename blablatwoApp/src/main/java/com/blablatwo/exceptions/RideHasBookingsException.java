package com.blablatwo.exceptions;

public class RideHasBookingsException extends RuntimeException {
    public RideHasBookingsException(Long rideId) {
        super("Ride " + rideId + " has active bookings and cannot be modified");
    }
}
