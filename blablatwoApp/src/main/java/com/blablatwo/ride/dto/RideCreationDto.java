package com.blablatwo.ride.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RideCreationDto(

        @NotNull(message = "Please provide origin city")
        Long originCityId,

        @NotNull(message = "Please provide destination city")
        Long destinationCityId,

        @NotNull(message = "Please provide departure time")
        @Future(message = "Departure time must be in the future")
        LocalDateTime departureTime,

        @Min(value = 1, message = "Available seats must be at least 1")
        int availableSeats,

        @DecimalMin(value = "0.0", inclusive = false, message = "Price per seat must be greater than 0")
        BigDecimal pricePerSeat,

        @NotNull(message = "Please provide vehicle")
        Long vehicleId,

        @NotNull(message = "Please provide stop city IDs")
        @Size(min = 1, message = "Please provide at least one stop city ID")
        List<Long> stopCityIds
) {}

