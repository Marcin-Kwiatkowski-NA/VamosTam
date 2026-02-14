package com.blablatwo.ride.dto;

import com.blablatwo.location.LocationRef;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Builder(toBuilder = true)
public record RideCreationDto(
        @NotNull(message = "Origin is required")
        @Valid LocationRef origin,

        @NotNull(message = "Destination is required")
        @Valid LocationRef destination,

        List<@Valid @NotNull IntermediateStopDto> intermediateStops,

        @NotNull(message = "Departure time cannot be null")
        @Future(message = "Departure time must be in the future")
        LocalDateTime departureTime,

        boolean isApproximate,

        @NotNull(message = "Available seats cannot be null")
        @Min(value = 1, message = "Available seats must be at least 1")
        int availableSeats,

        @DecimalMin(value = "0.0", inclusive = true, message = "Price per seat cannot be negative")
        BigDecimal pricePerSeat,

        Long vehicleId,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description
) {
}
