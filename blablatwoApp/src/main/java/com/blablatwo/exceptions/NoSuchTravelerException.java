package com.blablatwo.exceptions;

public class NoSuchTravelerException extends RuntimeException {
    public NoSuchTravelerException(Long travelerId) {
        super("No traveler found with id: " + travelerId);
    }
}
