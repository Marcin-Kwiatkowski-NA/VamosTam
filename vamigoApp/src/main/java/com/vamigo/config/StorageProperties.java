package com.vamigo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String bucket,
        String publicUrlBase,
        int avatarPresignTtlMinutes,
        long avatarMaxSizeBytes
) {}
