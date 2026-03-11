package com.vamigo.seat.dto;

import com.vamigo.domain.Currency;
import com.vamigo.location.LocationRef;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record SeatCreationDto(
        @NotNull(message = "Origin is required")
        @Valid LocationRef origin,

        @NotNull(message = "Destination is required")
        @Valid LocationRef destination,

        @NotNull(message = "Departure time cannot be null")
        @Future(message = "Departure time must be in the future")
        Instant departureTime,

        boolean isTimeApproximate,

        @Min(value = 1, message = "Count must be at least 1")
        int count,

        @DecimalMin(value = "0.0", inclusive = true, message = "Price cannot be negative")
        @DecimalMax(value = "9999.99", inclusive = true, message = "Price cannot exceed 9999.99")
        BigDecimal priceWillingToPay,

        @Size(max = 500, message = "Description cannot exceed 500 characters")
        String description,

        @Size(max = 32, message = "Contact phone cannot exceed 32 characters")
        String contactPhone,

        @NotNull(message = "Currency is required")
        Currency currency
) {
}
