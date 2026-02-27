package com.blablatwo.ride;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ride")
public record RideBusinessProperties(
        int autoCompleteBufferMinutes,
        int noBookingExpiryMinutes
) {
}
