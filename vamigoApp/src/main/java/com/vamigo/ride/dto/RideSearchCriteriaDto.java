package com.vamigo.ride.dto;

import jakarta.validation.constraints.Min;

import java.time.Instant;

public record RideSearchCriteriaDto(
        Long originOsmId,
        Long destinationOsmId,
        Double originLat,
        Double originLon,
        Double destinationLat,
        Double destinationLon,
        Double radiusKm,
        Instant earliestDeparture,
        Instant latestDeparture,
        @Min(1) Integer minAvailableSeats
) {

    public boolean isProximityMode() {
        return originLat != null && originLon != null
                && destinationLat != null && destinationLon != null;
    }
}
