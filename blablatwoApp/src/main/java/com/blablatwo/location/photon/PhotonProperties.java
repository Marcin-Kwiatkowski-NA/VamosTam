package com.blablatwo.location.photon;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PhotonProperties(
        @JsonProperty("osm_id") Long osmId,
        @JsonProperty("osm_key") String osmKey,
        @JsonProperty("osm_value") String osmValue,
        String type,
        String countrycode,
        String name,
        String state,
        String country,
        String county,
        String city,
        String postcode
) {
}
