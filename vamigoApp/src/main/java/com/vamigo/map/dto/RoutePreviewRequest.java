package com.vamigo.map.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RoutePreviewRequest(
        @NotNull
        @Size(min = 2, max = 20, message = "Must have between 2 and 20 stops")
        List<@Valid @NotNull CoordinateDto> stops
) {

    public record CoordinateDto(
            @NotNull(message = "Latitude is required")
            Double latitude,

            @NotNull(message = "Longitude is required")
            Double longitude,

            String name
    ) {}
}
