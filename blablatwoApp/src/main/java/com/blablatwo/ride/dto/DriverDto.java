package com.blablatwo.ride.dto;

public record DriverDto(
        String name,           // Traveler.name OR RideExternalMeta.authorName
        Double rating,         // null for now (future feature)
        Integer completedRides // null for now (future feature)
) {
}
