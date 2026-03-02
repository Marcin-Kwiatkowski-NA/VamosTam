package com.vamigo.search;

public record SearchConfigDto(
        double smartMatchRadiusKm,
        int smartMatchMaxSegments,
        int smartMatchRefreshCooldownSeconds
) {
}
