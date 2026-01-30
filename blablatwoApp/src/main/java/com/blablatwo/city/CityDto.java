package com.blablatwo.city;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO for city data in ride creation and responses.
 * <p>
 * For ride creation (mobile app): placeId is REQUIRED (from autocomplete selection).
 * For responses: name is localized based on request language.
 *
 * @param placeId     Unique identifier from geocoding provider (required for creation)
 * @param name        Display name (localized for responses, provided by client for creation)
 * @param countryCode ISO 3166-1 alpha-2 country code (optional)
 * @param population  City population (optional)
 */
public record CityDto(
        @NotNull Long placeId,
        @NotBlank String name,
        String countryCode,
        Long population
) {
}
