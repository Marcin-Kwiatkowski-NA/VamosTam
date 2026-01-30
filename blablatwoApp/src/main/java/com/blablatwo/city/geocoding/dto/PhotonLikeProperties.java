package com.blablatwo.city.geocoding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Properties from the Photon-like (GeoNames-based) geocoding API response.
 */
public record PhotonLikeProperties(
        @JsonProperty("geonameid") Long geonameid,
        String name,
        String lang,
        @JsonProperty("country_code") String countryCode,
        Long population
) {
}
