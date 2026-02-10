package com.blablatwo.ride.dto;

import com.blablatwo.city.Lang;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for external ride ingestion (scraper microservice).
 * <p>
 * City names are accepted and resolved to placeId by the backend.
 * If lang is provided, geocoding uses that language; otherwise, tries both pl and en.
 */
@Builder(toBuilder = true)
public record ExternalRideCreationDto(
        @NotBlank(message = "Origin city name is required")
        String originCityName,

        @NotBlank(message = "Destination city name is required")
        String destinationCityName,

        Lang lang,

        @NotNull(message = "Departure date is required")
        LocalDate departureDate,

        @NotNull(message = "Departure time is required")
        LocalTime departureTime,

        boolean isApproximate,

        int availableSeats,

        BigDecimal pricePerSeat,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @NotBlank(message = "External ID is required")
        String externalId,

        String rawContent,

        @Size(max = 32, message = "Phone number cannot exceed 32 characters")
        String phoneNumber,

        @Size(max = 100, message = "Author name cannot exceed 100 characters")
        String authorName,

        @NotBlank(message = "Source URL is required")
        String sourceUrl
) {
}
