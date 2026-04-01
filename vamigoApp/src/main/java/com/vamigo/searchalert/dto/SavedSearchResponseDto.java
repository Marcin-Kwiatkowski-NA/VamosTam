package com.vamigo.searchalert.dto;

import com.vamigo.searchalert.SearchType;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;

@Builder
public record SavedSearchResponseDto(
        Long id,
        String label,
        String originName,
        String destinationName,
        Long originOsmId,
        Double originLat,
        Double originLon,
        Long destinationOsmId,
        Double destinationLat,
        Double destinationLon,
        LocalDate departureDate,
        SearchType searchType,
        Integer minAvailableSeats,
        boolean autoCreated,
        boolean active,
        Instant createdAt,
        Instant lastPushSentAt,
        Instant lastEmailSentAt
) {
}
