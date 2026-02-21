package com.blablatwo.exceptions;

public class CannotBookOwnRideException extends RuntimeException {
    public CannotBookOwnRideException(Long rideId) {
        super("Cannot book your own ride (rideId=" + rideId + ")");
    }
}