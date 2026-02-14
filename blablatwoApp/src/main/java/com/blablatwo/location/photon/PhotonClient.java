package com.blablatwo.location.photon;

import com.blablatwo.config.CacheConfig;
import com.blablatwo.exceptions.GeocodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.List;

@Component
public class PhotonClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhotonClient.class);

    private final RestClient restClient;

    public PhotonClient(@Value("${photon.url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Cacheable(value = CacheConfig.PHOTON_SEARCH_CACHE, key = "#query + '|' + #limit")
    public List<PhotonFeature> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        LOGGER.debug("Calling Photon API: query='{}', limit={}", query, limit);

        PhotonResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api")
                            .queryParam("q", query)
                            .queryParam("limit", limit)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PhotonResponse.class);
        } catch (RestClientException e) {
            LOGGER.error("Failed to call Photon API for query '{}': {}", query, e.getMessage());
            throw new GeocodingException("Photon geocoding service unavailable", e);
        }

        if (response == null || response.features() == null || response.features().isEmpty()) {
            LOGGER.debug("No results found for query '{}'", query);
            return Collections.emptyList();
        }

        return response.features().stream()
                .filter(f -> f.properties().osmId() != null)
                .toList();
    }
}
