package com.blablatwo.ride.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RideCreationDto(
        @NotNull(message = "Driver ID cannot be null")
        Long driverId,

        @NotNull(message = "Origin place ID cannot be null")
        Long originPlaceId,

        @NotNull(message = "Destination place ID cannot be null")
        Long destinationPlaceId,

        @Future(message = "Departure time must be in the future")
        LocalDateTime departureTime,

        // If true, the departure time is approximate (UI determines display label)
        boolean isApproximate,

        @NotNull(message = "Available seats cannot be null")
        @Min(value = 1, message = "Available seats must be at least 1")
        int availableSeats,

        @NotNull(message = "Price per seat cannot be null")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price per seat cannot be negative")
        BigDecimal pricePerSeat,

        Long vehicleId,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description
) {
}