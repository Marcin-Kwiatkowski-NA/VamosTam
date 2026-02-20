package com.blablatwo.exceptions;

public class NotRideDriverException extends RuntimeException {
    public NotRideDriverException(Long rideId, Long userId) {
        super("User " + userId + " is not the driver of ride " + rideId);
    }
}
