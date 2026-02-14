package com.blablatwo.seat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Builder(toBuilder = true)
public record ExternalSeatCreationDto(
        @NotBlank(message = "Origin location name is required")
        String originLocationName,

        @NotBlank(message = "Destination location name is required")
        String destinationLocationName,

        @NotNull(message = "Departure date is required")
        LocalDate departureDate,

        @NotNull(message = "Departure time is required")
        LocalTime departureTime,

        boolean isApproximate,

        @Min(value = 1, message = "Count must be at least 1")
        int count,

        BigDecimal priceWillingToPay,

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
