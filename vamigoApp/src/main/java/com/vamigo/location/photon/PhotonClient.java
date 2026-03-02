package com.vamigo.location.photon;

import com.vamigo.config.CacheConfig;
import com.vamigo.exceptions.GeocodingException;
import com.vamigo.location.LocationLang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PhotonClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhotonClient.class);

    private final RestClient restClient;

    public PhotonClient(@Value("${photon.url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Cacheable(value = CacheConfig.PHOTON_SEARCH_CACHE, key = "#query + '|' + #limit + '|' + #lang")
    public List<PhotonFeature> search(String query, int limit, LocationLang lang) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        LOGGER.debug("Calling Photon API: query='{}', limit={}, lang={}", query, limit, lang);

        PhotonResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api")
                            .queryParam("q", query)
                            .queryParam("limit", limit)
                            .queryParam("lang", lang.name())
                            .queryParam("layer", "city")
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

        Set<String> seenNames = new HashSet<>();
        return response.features().stream()
                .filter(f -> f.properties().osmId() != null)
                .filter(f -> seenNames.add(f.properties().name()))
                .toList();
    }

    public List<PhotonFeature> reverse(double lat, double lon, LocationLang lang) {
        LOGGER.debug("Calling Photon reverse API: lat={}, lon={}, lang={}", lat, lon, lang);

        PhotonResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/reverse")
                            .queryParam("lat", lat)
                            .queryParam("lon", lon)
                            .queryParam("lang", lang.name())
                            .queryParam("layer", "city")
                            .queryParam("limit", 6)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PhotonResponse.class);
        } catch (RestClientException e) {
            LOGGER.error("Failed to call Photon reverse API for lat={}, lon={}: {}", lat, lon, e.getMessage());
            throw new GeocodingException("Photon reverse geocoding service unavailable", e);
        }

        if (response == null || response.features() == null || response.features().isEmpty()) {
            LOGGER.debug("No reverse results for lat={}, lon={}", lat, lon);
            return Collections.emptyList();
        }

        return response.features().stream()
                .filter(f -> f.properties().osmId() != null)
                .toList();
    }
}
