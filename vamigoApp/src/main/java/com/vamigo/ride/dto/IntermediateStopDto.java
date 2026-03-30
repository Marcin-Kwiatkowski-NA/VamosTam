package com.vamigo.ride.dto;

import com.vamigo.location.LocationRef;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;

public record IntermediateStopDto(
        @NotNull @Valid LocationRef location,
        @Future Instant departureTime,

        @DecimalMin(value = "0.0", inclusive = true, message = "Leg price cannot be negative")
        @DecimalMax(value = "9999.99", inclusive = true, message = "Leg price cannot exceed 9999.99")
        BigDecimal legPrice
) {
}
