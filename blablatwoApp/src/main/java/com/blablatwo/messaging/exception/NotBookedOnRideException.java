package com.blablatwo.messaging.exception;

public class NotBookedOnRideException extends RuntimeException {
    public NotBookedOnRideException(Long rideId, Long passengerId) {
        super("Passenger is not booked on this ride");
    }
}
