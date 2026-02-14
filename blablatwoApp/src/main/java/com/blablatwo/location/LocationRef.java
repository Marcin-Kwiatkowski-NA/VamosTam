package com.blablatwo.location;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LocationRef(
        @NotNull Long osmId,
        @NotBlank String name,
        @NotNull Double latitude,
        @NotNull Double longitude,
        String countryCode,
        String country,
        String state,
        String county,
        String city,
        String postCode,
        String type,
        String osmKey,
        String osmValue
) {
}
