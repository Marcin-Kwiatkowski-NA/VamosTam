package com.blablatwo.ride.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record BookRideRequest(
        @NotNull(message = "Board stop place ID cannot be null")
        Long boardStopPlaceId,

        @NotNull(message = "Alight stop place ID cannot be null")
        Long alightStopPlaceId
) {
}
