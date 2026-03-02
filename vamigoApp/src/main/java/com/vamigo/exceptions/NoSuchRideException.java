package com.vamigo.exceptions;

public class NoSuchRideException extends RuntimeException {
    public NoSuchRideException(Long rideId) {
        super("No ride found with id: " + rideId);
    }
}
