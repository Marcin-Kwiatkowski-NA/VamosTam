package com.vamigo.exceptions;

public class RideDepartedException extends RuntimeException {
    public RideDepartedException(Long rideId) {
        super("Ride " + rideId + " has already departed. Cancellation is not allowed after departure time.");
    }
}
