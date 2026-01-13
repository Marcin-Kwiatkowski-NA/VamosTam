package com.blablatwo.city.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PhotonProperties(
        @JsonProperty("osm_id") Long osmId,
        String name
) {
}
