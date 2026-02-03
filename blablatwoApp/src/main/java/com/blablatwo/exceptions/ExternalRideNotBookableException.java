package com.blablatwo.exceptions;

public class ExternalRideNotBookableException extends RuntimeException {
    public ExternalRideNotBookableException(Long rideId) {
        super("External ride " + rideId + " cannot be booked or cancelled through this system");
    }
}
