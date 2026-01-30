package com.blablatwo.city.geocoding;

import com.blablatwo.city.geocoding.dto.PhotonLikeFeature;
import com.blablatwo.city.geocoding.dto.PhotonLikeProperties;
import com.blablatwo.city.geocoding.dto.PhotonLikeResponse;
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
import java.util.Optional;

/**
 * Geocoding client implementation using a Photon-like API backed by GeoNames data.
 * Results are cached using Caffeine with a 3-day TTL.
 */
@Component
public class PhotonLikeGeocodingClient implements GeocodingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhotonLikeGeocodingClient.class);

    private final RestClient restClient;

    public PhotonLikeGeocodingClient(@Value("${geocoding.photon-like.url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    @Cacheable(value = CacheConfig.GEOCODING_CACHE, key = "#query + '|' + #lang + '|' + #limit")
    public List<GeocodedPlace> search(String query, String lang, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        LOGGER.debug("Calling Photon-like API: query='{}', lang='{}', limit={}", query, lang, limit);

        PhotonLikeResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api")
                            .queryParam("q", query)
                            .queryParam("lang", lang)
                            .queryParam("limit", limit)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PhotonLikeResponse.class);
        } catch (RestClientException e) {
            LOGGER.error("Failed to call Photon-like API for query '{}': {}", query, e.getMessage());
            throw new GeocodingException("Geocoding service unavailable", e);
        }

        if (response == null || response.features() == null || response.features().isEmpty()) {
            LOGGER.debug("No results found for query '{}'", query);
            return Collections.emptyList();
        }

        return response.features().stream()
                .map(this::toGeocodedPlace)
                .filter(place -> place.placeId() != null)
                .toList();
    }

    @Override
    @Cacheable(value = CacheConfig.GEOCODING_CACHE, key = "'placeId:' + #placeId + '|' + #lang")
    public Optional<GeocodedPlace> lookupByPlaceId(Long placeId, String lang) {
        if (placeId == null) {
            return Optional.empty();
        }

        String effectiveLang = (lang == null || lang.isBlank()) ? "pl" : lang;

        LOGGER.debug("Looking up place by placeId: {}, lang: {}", placeId, effectiveLang);

        PhotonLikeResponse response;
        try {
            response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api")
                            .queryParam("placeId", placeId)
                            .queryParam("lang", effectiveLang)
                            .queryParam("limit", 1)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PhotonLikeResponse.class);
        } catch (RestClientException e) {
            LOGGER.error("Failed to lookup placeId {}: {}", placeId, e.getMessage());
            throw new GeocodingException("Geocoding service unavailable", e);
        }

        if (response == null || response.features() == null || response.features().isEmpty()) {
            LOGGER.debug("No place found for placeId: {}", placeId);
            return Optional.empty();
        }

        return Optional.of(toGeocodedPlace(response.features().get(0)));
    }

    private GeocodedPlace toGeocodedPlace(PhotonLikeFeature feature) {
        PhotonLikeProperties props = feature.properties();
        return new GeocodedPlace(
                props.geonameid(),
                props.name(),
                props.lang(),
                props.countryCode(),
                props.population()
        );
    }
}
