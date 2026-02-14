package com.blablatwo.ride.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record RideSearchCriteriaDto(
        Long originOsmId,
        Long destinationOsmId,
        LocalDate departureDate,
        LocalDate departureDateTo,
        LocalTime departureTimeFrom,
        int minAvailableSeats
) {
}
