package com.blablatwo.search;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "search")
public record SearchProperties(
        Proximity proximity,
        SmartMatch smartMatch
) {

    public record Proximity(
            int radiusDivisor,
            double minRadiusKm
    ) {
    }

    public record SmartMatch(
            double radiusKm,
            int maxSegments,
            int refreshCooldownSeconds
    ) {
    }
}
