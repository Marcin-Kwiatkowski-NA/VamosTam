package com.blablatwo.seat.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public record SeatSearchCriteriaDto(
        Long originOsmId,
        Long destinationOsmId,
        LocalDate departureDate,
        LocalDate departureDateTo,
        LocalTime departureTimeFrom,
        Integer availableSeatsInCar
) {
}
