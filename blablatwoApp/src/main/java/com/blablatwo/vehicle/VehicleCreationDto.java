package com.blablatwo.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

public record VehicleCreationDto(
        String make,
        String model,
        Integer productionYear,
        String color,
        String licensePlate
) {}