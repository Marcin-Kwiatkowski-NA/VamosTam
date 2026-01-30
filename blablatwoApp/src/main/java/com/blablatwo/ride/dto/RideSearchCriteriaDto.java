package com.blablatwo.ride.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for ride search criteria.
 * <p>
 * Filtering uses placeId for exact city matching.
 * The lang parameter controls response localization only (not filtering).
 *
 * @param originPlaceId      Filter by origin city's placeId (exact match)
 * @param destinationPlaceId Filter by destination city's placeId (exact match)
 * @param lang               Language for response localization ("pl" or "en")
 * @param departureDate      Filter rides departing on or after this date
 * @param departureDateTo    Filter rides departing on or before this date
 * @param departureTimeFrom  Filter rides departing at or after this time (on departureDate)
 * @param minAvailableSeats  Filter rides with at least this many available seats
 */
public record RideSearchCriteriaDto(
        Long originPlaceId,
        Long destinationPlaceId,
        String lang,
        LocalDate departureDate,
        LocalDate departureDateTo,
        LocalTime departureTimeFrom,
        int minAvailableSeats
) {
}
