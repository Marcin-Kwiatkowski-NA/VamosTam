package com.blablatwo.ride.dto;

import com.blablatwo.location.LocationRef;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record IntermediateStopDto(
        @NotNull @Valid LocationRef location,
        @NotNull @Future LocalDateTime departureTime
) {
}
