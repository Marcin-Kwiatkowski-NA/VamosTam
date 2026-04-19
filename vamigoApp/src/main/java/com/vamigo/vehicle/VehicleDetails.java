package com.vamigo.vehicle;

public record VehicleDetails(
        String make,
        String model,
        Integer productionYear,
        VehicleColor color,
        String licensePlate,
        String description
) {
}
