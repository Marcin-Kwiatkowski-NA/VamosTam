package com.blablatwo.ride.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RideCreationDto(

        @NotNull(message = "Please provide origin city")
        String origin,

        @NotNull(message = "Please provide destination city")
        String destination,

        @NotNull(message = "Please provide departure time")
        @Future(message = "Departure time must be in the future")
        LocalDateTime departureTime,

        @Min(value = 1, message = "Available seats must be at least 1")
        int availableSeats,

        @DecimalMin(value = "0.0", inclusive = false, message = "Price per seat must be greater than 0")
        BigDecimal pricePerSeat,

        @NotNull(message = "Please provide vehicle")
        Long vehicleId
) {}

