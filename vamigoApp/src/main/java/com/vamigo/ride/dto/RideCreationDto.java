package com.vamigo.ride.dto;

import com.vamigo.location.LocationRef;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
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
        Instant departureTime,

        boolean isTimeApproximate,

        @NotNull(message = "Available seats cannot be null")
        @Min(value = 1, message = "Available seats must be at least 1")
        int availableSeats,

        @DecimalMin(value = "0.0", inclusive = true, message = "Price per seat cannot be negative")
        BigDecimal pricePerSeat,

        Long vehicleId,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        boolean autoApprove,

        @Size(max = 32, message = "Contact phone cannot exceed 32 characters")
        String contactPhone
) {
    public RideCreationDto {
        // Default autoApprove to true if not explicitly set
    }
}
