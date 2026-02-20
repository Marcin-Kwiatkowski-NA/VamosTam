package com.blablatwo.ride.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record BookRideRequest(
        @NotNull(message = "Board stop OSM ID cannot be null")
        Long boardStopOsmId,

        @NotNull(message = "Alight stop OSM ID cannot be null")
        Long alightStopOsmId,

        @Min(value = 1, message = "Seat count must be at least 1")
        int seatCount
) {
    public BookRideRequest {
        if (seatCount <= 0) seatCount = 1;
    }
}
