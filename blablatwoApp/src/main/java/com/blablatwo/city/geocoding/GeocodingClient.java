package com.blablatwo.city.geocoding;

import java.util.List;
import java.util.Optional;

/**
 * Abstraction for geocoding providers.
 * Implementations should handle caching and error handling.
 */
public interface GeocodingClient {

    /**
     * Search for places matching the query.
     *
     * @param query Search query (city name)
     * @param lang  Language code for results (e.g., "pl", "en")
     * @param limit Maximum number of results to return
     * @return List of matching places, empty if none found
     */
    List<GeocodedPlace> search(String query, String lang, int limit);

    /**
     * Find the best matching city for the query.
     *
     * @param query Search query (city name)
     * @param lang  Language code for results (e.g., "pl", "en")
     * @return The best matching city, or empty if not found
     */
    default Optional<GeocodedPlace> bestMatchCity(String query, String lang) {
        List<GeocodedPlace> results = search(query, lang, 1);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Lookup a place by its placeId (geonameid).
     *
     * @param placeId The geocoding provider's place ID
     * @param lang    Language code for results (e.g., "pl", "en")
     * @return The place details, or empty if not found
     */
    Optional<GeocodedPlace> lookupByPlaceId(Long placeId, String lang);
}
