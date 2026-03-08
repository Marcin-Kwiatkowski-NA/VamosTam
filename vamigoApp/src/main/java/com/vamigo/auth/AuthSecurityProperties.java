package com.vamigo.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public record AuthSecurityProperties(
        RateLimit rateLimit,
        AccountLock accountLock
) {
    public record RateLimit(int requestsPerMinute) {}
    public record AccountLock(int maxFailedAttempts, int lockDurationMinutes) {}
}
