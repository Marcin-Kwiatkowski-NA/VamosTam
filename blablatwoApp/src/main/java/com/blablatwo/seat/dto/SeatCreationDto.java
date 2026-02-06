package com.blablatwo.seat.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SeatCreationDto(
        @NotNull(message = "Origin place ID cannot be null")
        Long originPlaceId,

        @NotNull(message = "Destination place ID cannot be null")
        Long destinationPlaceId,

        @NotNull(message = "Departure time cannot be null")
        @Future(message = "Departure time must be in the future")
        LocalDateTime departureTime,

        boolean isApproximate,

        @Min(value = 1, message = "Count must be at least 1")
        int count,

        BigDecimal priceWillingToPay,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description
) {
}
