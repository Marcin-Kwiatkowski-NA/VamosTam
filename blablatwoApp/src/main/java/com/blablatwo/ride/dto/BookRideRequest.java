package com.blablatwo.ride.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record BookRideRequest(
        @NotNull(message = "Board stop OSM ID cannot be null")
        Long boardStopOsmId,

        @NotNull(message = "Alight stop OSM ID cannot be null")
        Long alightStopOsmId
) {
}
