package com.blablatwo.city.geocoding;

/**
 * Provider-agnostic representation of a geocoded place.
 *
 * @param placeId     Unique identifier from the geocoding provider (e.g., geonameid)
 * @param name        Place name in the requested language
 * @param lang        Language code of the name (e.g., "pl", "en")
 * @param countryCode ISO 3166-1 alpha-2 country code (e.g., "PL", "DE")
 * @param population  Population of the place (may be null)
 */
public record GeocodedPlace(
        Long placeId,
        String name,
        String lang,
        String countryCode,
        Long population
) {
}
