package com.blablatwo.exceptions;

public class BookingNotFoundException extends RuntimeException {
    public BookingNotFoundException(Long rideId, Long passengerId) {
        super("Passenger " + passengerId + " has no booking on ride " + rideId);
    }
}
