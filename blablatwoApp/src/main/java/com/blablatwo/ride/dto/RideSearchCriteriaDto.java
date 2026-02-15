package com.blablatwo.ride.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record RideSearchCriteriaDto(
        Long originOsmId,
        Long destinationOsmId,
        Double originLat,
        Double originLon,
        Double destinationLat,
        Double destinationLon,
        Double radiusKm,
        Long excludeOriginOsmId,
        Long excludeDestinationOsmId,
        LocalDate departureDate,
        LocalDate departureDateTo,
        LocalTime departureTimeFrom,
        int minAvailableSeats
) {

    public boolean isProximityMode() {
        return originLat != null && originLon != null
                && destinationLat != null && destinationLon != null;
    }
}
