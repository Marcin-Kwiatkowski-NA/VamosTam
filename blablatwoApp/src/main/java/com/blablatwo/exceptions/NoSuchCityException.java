package com.blablatwo.exceptions;

public class NoSuchCityException extends RuntimeException {

    public NoSuchCityException(String cityName) {
        super("No city found with name: " + cityName);
    }
}
