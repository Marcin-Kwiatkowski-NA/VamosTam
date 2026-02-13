package com.blablatwo.exceptions;

public class SegmentFullException extends RuntimeException {
    public SegmentFullException(Long rideId, int fromOrder, int toOrder) {
        super("Ride " + rideId + " segment " + fromOrder + " -> " + toOrder + " is full");
    }
}
