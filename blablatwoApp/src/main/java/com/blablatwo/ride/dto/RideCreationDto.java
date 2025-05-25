package com.blablatwo.ride.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RideCreationDto(
        @NotNull(message = "Driver ID cannot be null")
        Long driverId,

        @NotBlank(message = "Origin city cannot be blank")
        @Size(max = 100, message = "Origin city name cannot exceed 100 characters")
        String origin,

        @NotBlank(message = "Destination city cannot be blank")
        @Size(max = 100, message = "Destination city name cannot exceed 100 characters")
        String destination,

        @NotNull(message = "Departure time cannot be null")
        @Future(message = "Departure time must be in the future")
        LocalDateTime departureTime,

        @Min(value = 1, message = "Available seats must be at least 1")
        int availableSeats,

        @NotNull(message = "Price per seat cannot be null")
        @Min(value = 0, message = "Price per seat cannot be negative")
        BigDecimal pricePerSeat,

        Long vehicleId
) {}