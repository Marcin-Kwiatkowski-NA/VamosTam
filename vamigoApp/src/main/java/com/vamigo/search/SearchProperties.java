package com.vamigo.search;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Search-related config consumed by the frontend via {@code /search/config}.
 * Backend matching radii live under {@code match.*} —
 * see {@link com.vamigo.match.MatchProperties}.
 */
@ConfigurationProperties(prefix = "search")
public record SearchProperties(SmartMatch smartMatch) {

    public record SmartMatch(
            double radiusKm,
            int maxSegments,
            int refreshCooldownSeconds,
            int defaultNearbyLimit
    ) {
    }
}
