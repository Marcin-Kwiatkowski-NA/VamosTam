package com.blablatwo.location;

public class NoSuchLocationException extends RuntimeException {

    public NoSuchLocationException(String locationName) {
        super("No location found with name: " + locationName);
    }

    public NoSuchLocationException(Long osmId) {
        super("Location with osmId=" + osmId + " not found.");
    }
}
