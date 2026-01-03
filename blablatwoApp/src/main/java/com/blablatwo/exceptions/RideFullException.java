package com.blablatwo.exceptions;

public class RideFullException extends RuntimeException {
    public RideFullException(Long rideId) {
        super("Ride with id " + rideId + " is full. No seats available.");
    }
}
