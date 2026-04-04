package com.vamigo.map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "map")
public record MapProperties(String osrmUrl) {}
