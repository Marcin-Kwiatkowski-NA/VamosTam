package com.blablatwo.exceptions;

public class InsufficientSeatsException extends RuntimeException {
    public InsufficientSeatsException(Long rideId, int requested, int available) {
        super("Ride " + rideId + ": requested " + requested + " seats but only " + available + " available");
    }
}
