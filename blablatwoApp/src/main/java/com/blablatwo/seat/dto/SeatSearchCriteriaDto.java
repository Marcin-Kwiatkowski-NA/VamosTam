package com.blablatwo.seat.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for seat search criteria.
 *
 * @param originPlaceId      Filter by origin city's placeId (exact match)
 * @param destinationPlaceId Filter by destination city's placeId (exact match)
 * @param lang               Language for response localization ("pl" or "en")
 * @param departureDate      Filter seats departing on or after this date
 * @param departureDateTo    Filter seats departing on or before this date
 * @param departureTimeFrom  Filter seats departing at or after this time (on departureDate)
 * @param availableSeatsInCar Optional: "I have N seats in my car" — filters seats where count <= N
 */
public record SeatSearchCriteriaDto(
        Long originPlaceId,
        Long destinationPlaceId,
        String lang,
        LocalDate departureDate,
        LocalDate departureDateTo,
        LocalTime departureTimeFrom,
        Integer availableSeatsInCar
) {
}
