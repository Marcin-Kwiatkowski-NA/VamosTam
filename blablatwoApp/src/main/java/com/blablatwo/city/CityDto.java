package com.blablatwo.city;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CityDto(
        @NotNull Long osmId,
        @NotBlank String name
) {}
