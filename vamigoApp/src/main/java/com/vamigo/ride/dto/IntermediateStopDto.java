package com.vamigo.ride.dto;

import com.vamigo.location.LocationRef;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record IntermediateStopDto(
        @NotNull @Valid LocationRef location,
        @NotNull @Future Instant departureTime
) {
}
