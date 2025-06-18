package com.blablatwo.ride.dto;

import com.blablatwo.city.CityDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RideCreationDto(
        @NotNull(message = "Driver ID cannot be null")
        Long driverId,

        @Valid @NotNull
        CityDto origin,
        @Valid @NotNull
        CityDto destination,

        @NotNull(message = "Departure time cannot be null")
        @Future(message = "Departure time must be in the future")
        LocalDateTime departureTime,

        @NotNull(message = "Available seats cannot be null")
        @Min(value = 1, message = "Available seats must be at least 1")
        int availableSeats,

        @NotNull(message = "Price per seat cannot be null")
        @DecimalMin(value = "0.0", inclusive = true, message = "Price per seat cannot be negative")
        BigDecimal pricePerSeat,

        Long vehicleId,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description // ADDED
) {}