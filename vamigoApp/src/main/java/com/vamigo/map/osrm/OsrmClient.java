package com.vamigo.map.osrm;

import com.vamigo.config.CacheConfig;
import com.vamigo.map.MapProperties;
import com.vamigo.map.RoutingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OsrmClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsrmClient.class);

    private final RestClient restClient;

    public OsrmClient(MapProperties mapProperties) {
        this.restClient = RestClient.builder()
                .baseUrl(mapProperties.osrmUrl())
                .build();
    }

    /**
     * Calls OSRM route service with the given coordinates.
     *
     * @param cacheKey    precomputed cache key: "{profile}|{lon,lat;lon,lat;...}"
     * @param coordinates ordered list of [lon, lat] pairs
     * @param profile     routing profile (e.g. "driving")
     * @return OSRM route response with GeoJSON geometry
     */
    @Cacheable(value = CacheConfig.OSRM_ROUTE_CACHE, key = "#cacheKey")
    public OsrmRouteResponse route(String cacheKey, List<double[]> coordinates, String profile) {
        String coordString = coordinates.stream()
                .map(c -> c[0] + "," + c[1])
                .collect(Collectors.joining(";"));

        LOGGER.debug("Calling OSRM route API: profile={}, coordinates={}", profile, coordString);

        try {
            return restClient.get()
                    .uri("/route/v1/{profile}/{coordinates}?overview=full&geometries=geojson&steps=false",
                            profile, coordString)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(OsrmRouteResponse.class);
        } catch (RestClientException e) {
            LOGGER.error("Failed to call OSRM route API: {}", e.getMessage());
            throw new RoutingException("OSRM routing service unavailable", e);
        }
    }

    /**
     * Builds a cache key from the routing profile and ordered coordinates.
     */
    public static String buildCacheKey(String profile, List<double[]> coordinates) {
        String coordPart = coordinates.stream()
                .map(c -> c[0] + "," + c[1])
                .collect(Collectors.joining(";"));
        return profile + "|" + coordPart;
    }
}
