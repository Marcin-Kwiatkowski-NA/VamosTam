package com.blablatwo.seat.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record SeatSearchCriteriaDto(
        Long originOsmId,
        Long destinationOsmId,
        Double originLat,
        Double originLon,
        Double destinationLat,
        Double destinationLon,
        Double radiusKm,
        LocalDate departureDate,
        LocalDate departureDateTo,
        LocalTime departureTimeFrom,
        Integer availableSeatsInCar
) {

    public boolean isProximityMode() {
        return originLat != null && originLon != null
                && destinationLat != null && destinationLon != null;
    }
}
