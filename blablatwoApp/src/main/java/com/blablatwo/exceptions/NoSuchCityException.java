package com.blablatwo.exceptions;

public class NoSuchCityException extends RuntimeException {

    public NoSuchCityException(String cityName) {
        super("No city found with name: " + cityName);
    }

    public NoSuchCityException(Long placeId) {
        super("City with placeId=" + placeId + " not found in database or geocoding service.");
    }
}
