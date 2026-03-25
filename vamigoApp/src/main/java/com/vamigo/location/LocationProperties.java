package com.vamigo.location;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "location")
public record LocationProperties(
        Map<String, Long> overrides
) {
    public LocationProperties {
        if (overrides == null) {
            overrides = Map.of();
        }
    }
}