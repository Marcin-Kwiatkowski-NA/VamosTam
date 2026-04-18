package com.vamigo.location;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.vamigo.AbstractFullStackTest;
import com.vamigo.config.CacheConfig;
import com.vamigo.exceptions.GeocodingException;
import com.vamigo.util.Constants;
import com.vamigo.util.PhotonStubs;
import com.vamigo.util.PhotonStubs.PhotonFeatureData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.wiremock.spring.InjectWireMock;

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@link LocationResolutionService#resolveByName(String)} against WireMock-stubbed
 * Photon. Verifies: (1) first call hits Photon + persists a row, (2) second call short-circuits
 * via Caffeine cache ({@code PHOTON_SEARCH_CACHE}) — 0 additional HTTP requests, (3) Photon 503
 * bubbles a {@link GeocodingException}.
 *
 * <p>WireMock request journal is reset per test by {@code wiremock-spring-boot}, so
 * {@code photonMock.verify(0, ...)} against this class' own stubs is meaningful.
 */
class LocationResolutionIT extends AbstractFullStackTest {

    @InjectWireMock("photon-mock")
    WireMockServer photonMock;

    @Autowired LocationResolutionService service;
    @Autowired LocationRepository locationRepository;
    @Autowired CacheManager cacheManager;

    @Test
    void resolveByName_firstCall_persistsLocation_andSecondCallHitsCache() {
        // Evict the Photon cache so this test doesn't ride on a cache entry from an earlier IT.
        var cache = cacheManager.getCache(CacheConfig.PHOTON_SEARCH_CACHE);
        if (cache != null) cache.clear();

        PhotonStubs.stubSearch(photonMock, "Kraków", PhotonFeatureData.krakow());
        PhotonStubs.stubReverseEmpty(photonMock);

        Location resolved = service.resolveByName("Kraków");
        assertThat(resolved.getOsmId()).isEqualTo(Constants.OSM_ID_KRAKOW);
        assertThat(locationRepository.findByOsmId(Constants.OSM_ID_KRAKOW)).isPresent();

        // Request journal — exactly one /api hit so far.
        photonMock.verify(1, getRequestedFor(urlPathEqualTo("/api"))
                .withQueryParam("q", equalTo("Kraków")));

        Location cached = service.resolveByName("Kraków");
        assertThat(cached.getId()).isEqualTo(resolved.getId());

        // Second call must not have added any /api requests — cache short-circuited PhotonClient.
        photonMock.verify(1, getRequestedFor(urlPathEqualTo("/api"))
                .withQueryParam("q", equalTo("Kraków")));
    }

    @Test
    void resolveByName_whenPhotonReturns503_raisesGeocodingException() {
        var cache = cacheManager.getCache(CacheConfig.PHOTON_SEARCH_CACHE);
        if (cache != null) cache.clear();

        PhotonStubs.stubSearchFails(photonMock, "Nonexistentville", 503);

        assertThatThrownBy(() -> service.resolveByName("Nonexistentville"))
                .isInstanceOf(GeocodingException.class);
    }
}
