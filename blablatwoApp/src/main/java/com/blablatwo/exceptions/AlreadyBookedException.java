package com.blablatwo.exceptions;

public class AlreadyBookedException extends RuntimeException {
    public AlreadyBookedException(Long rideId, Long passengerId) {
        super("Passenger " + passengerId + " already booked on ride " + rideId);
    }
}
