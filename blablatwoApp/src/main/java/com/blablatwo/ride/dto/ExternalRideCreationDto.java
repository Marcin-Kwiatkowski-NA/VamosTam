package com.blablatwo.ride.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public record ExternalRideCreationDto(
        @NotBlank(message = "Origin city name is required")
        String originCityName,

        @NotBlank(message = "Destination city name is required")
        String destinationCityName,

        @NotNull(message = "Departure date is required")
        LocalDate departureDate,

        @NotNull(message = "Departure time is required")
        LocalTime departureTime,

        Boolean isApproximate,

        Integer availableSeats,

        BigDecimal pricePerSeat,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @NotBlank(message = "External ID is required")
        String externalId,

        String rawContent,

        @Size(max = 32, message = "Phone number cannot exceed 32 characters")
        String phoneNumber,

        @NotBlank(message = "Source URL is required")
        String sourceUrl
) {
}
