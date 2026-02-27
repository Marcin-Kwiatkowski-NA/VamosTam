package com.blablatwo.search;

public record SearchConfigDto(
        double smartMatchRadiusKm,
        int smartMatchMaxSegments,
        int smartMatchRefreshCooldownSeconds
) {
}
