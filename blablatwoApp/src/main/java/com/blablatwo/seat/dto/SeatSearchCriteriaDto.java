package com.blablatwo.seat.dto;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record SeatSearchCriteriaDto(
        Long originOsmId,
        Long destinationOsmId,
        Double originLat,
        Double originLon,
        Double destinationLat,
        Double destinationLon,
        Double radiusKm,
        @NotNull Instant earliestDeparture,
        Instant latestDeparture,
        Integer availableSeatsInCar
) {

    public boolean isProximityMode() {
        return originLat != null && originLon != null
                && destinationLat != null && destinationLon != null;
    }
}
