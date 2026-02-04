package com.blablatwo.ride.dto;

public record DriverDto(
        Long id,               // UserAccount.id or FACEBOOK_PROXY_ID for external rides
        String name,           // UserProfile.displayName OR RideExternalMeta.authorName
        Double rating,         // null for now (future feature)
        Integer completedRides // null for now (future feature)
) {
}
