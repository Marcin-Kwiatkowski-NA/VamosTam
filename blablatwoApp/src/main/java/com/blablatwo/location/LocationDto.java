package com.blablatwo.location;

public record LocationDto(
        Long osmId,
        String name,
        String country,
        String countryCode,
        Double latitude,
        Double longitude,
        String type
) {
}
