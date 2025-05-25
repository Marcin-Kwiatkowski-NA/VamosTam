package com.blablatwo.city;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CityCreationDto(
        @NotBlank(message = "City name cannot be blank")
        @Size(max = 100, message = "City name cannot exceed 100 characters")
        String name
) {}