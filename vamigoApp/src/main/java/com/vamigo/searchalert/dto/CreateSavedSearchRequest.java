package com.vamigo.searchalert.dto;

import com.vamigo.searchalert.SearchType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateSavedSearchRequest(
        @NotNull Long originOsmId,
        @NotBlank @Size(max = 200) String originName,
        @NotNull Double originLat,
        @NotNull Double originLon,
        @NotNull Long destinationOsmId,
        @NotBlank @Size(max = 200) String destinationName,
        @NotNull Double destinationLat,
        @NotNull Double destinationLon,
        @NotNull LocalDate departureDate,
        @NotNull SearchType searchType,
        Integer minAvailableSeats
) {
}
