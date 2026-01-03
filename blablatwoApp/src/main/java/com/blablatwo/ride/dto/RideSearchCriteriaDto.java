package com.blablatwo.ride.dto;

import java.time.LocalDate;

public record RideSearchCriteriaDto(
        String originCityName,
        String destinationCityName,
        LocalDate departureDate,
        LocalDate departureDateTo,
        Integer minAvailableSeats
) {
}
