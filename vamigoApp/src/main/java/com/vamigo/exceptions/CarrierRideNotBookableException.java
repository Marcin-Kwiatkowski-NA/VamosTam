package com.vamigo.exceptions;

public class CarrierRideNotBookableException extends RuntimeException {

    public CarrierRideNotBookableException(Long rideId) {
        super("Cannot book rides offered by carriers directly. Ride ID: " + rideId);
    }
}
