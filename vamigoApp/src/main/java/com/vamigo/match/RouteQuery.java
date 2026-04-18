package com.vamigo.match;

/**
 * Route query: origin and destination as coordinates plus optional OSM ids.
 * When both osmIds are present the matcher can take an exact-match fast path.
 */
public record RouteQuery(
        GeoPoint origin,
        GeoPoint destination,
        Long originOsmId,
        Long destinationOsmId
) {

    public boolean hasExactOsmIds() {
        return originOsmId != null && destinationOsmId != null;
    }
}
