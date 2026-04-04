package com.vamigo.seat.dto;

import java.time.Instant;

public record SeatSearchCriteriaDto(
        Long originOsmId,
        Long destinationOsmId,
        Double originLat,
        Double originLon,
        Double destinationLat,
        Double destinationLon,
        Double radiusKm,
        Instant earliestDeparture,
        Instant latestDeparture,
        Integer availableSeatsInCar
) {

    public boolean isProximityMode() {
        return originLat != null && originLon != null
                && destinationLat != null && destinationLon != null;
    }
}
