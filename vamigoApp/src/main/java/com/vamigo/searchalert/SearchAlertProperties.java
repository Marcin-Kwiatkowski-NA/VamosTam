package com.vamigo.searchalert;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "search-alert")
public record SearchAlertProperties(
        long pushCheckIntervalMs,
        int pushIntervalHours,
        String emailCron,
        int maxAlertsPerUser,
        double proximityRadiusKm
) {
    public double proximityRadiusMeters() {
        return proximityRadiusKm * 1000;
    }
}
