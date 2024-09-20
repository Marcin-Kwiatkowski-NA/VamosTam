package com.blablatwo.exceptions;

public class NoSuchRideException extends RuntimeException{
    public NoSuchRideException(long rideId) {
        super("No ride found with id: " + rideId);
    }
}
