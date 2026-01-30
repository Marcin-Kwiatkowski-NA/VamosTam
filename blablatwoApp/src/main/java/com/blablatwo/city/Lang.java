package com.blablatwo.city;

/**
 * Supported languages for city name resolution and display.
 */
public enum Lang {
    PL("pl"),
    EN("en");

    private final String code;

    Lang(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
