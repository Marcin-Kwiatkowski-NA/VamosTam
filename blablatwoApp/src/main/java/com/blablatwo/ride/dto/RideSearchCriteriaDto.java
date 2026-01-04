package com.blablatwo.ride.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record RideSearchCriteriaDto(
        String originCityName,
        String destinationCityName,
        LocalDate departureDate,
        LocalDate departureDateTo,
        LocalTime departureTimeFrom,
        Integer minAvailableSeats
) {
}
