package com.vamigo.search;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external-import")
public record ExternalImportProperties(
        String notifyAddress,
        int minMatchingResults
) {
}
