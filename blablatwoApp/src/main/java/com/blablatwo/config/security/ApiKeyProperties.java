package com.blablatwo.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.api-key")
public record ApiKeyProperties(String externalRides) {
}
