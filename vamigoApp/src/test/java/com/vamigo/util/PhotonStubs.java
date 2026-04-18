package com.vamigo.util;

import com.github.tomakehurst.wiremock.WireMockServer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

/**
 * WireMock stubs for the Photon geocoding API.
 *
 * <p>GeoJSON returned by these helpers includes <b>every field</b> {@code PhotonProperties} parses
 * ({@code osm_id}, {@code osm_key}, {@code osm_value}, {@code type}, {@code name}, {@code state},
 * {@code country}, {@code city}, {@code postcode}, {@code countrycode}) plus a full
 * {@code geometry.coordinates} [lon, lat] pair. Partial GeoJSON parses to {@code null osmId} and
 * fails deduplication silently, so partial stubs are intentionally forbidden here.
 *
 * <p>OSM IDs line up with {@link Constants} (e.g. {@code OSM_ID_KRAKOW}) so stubbed features match
 * any pre-seeded {@link com.vamigo.location.Location} rows.
 */
public final class PhotonStubs {

    private PhotonStubs() {}

    public static void stubSearch(WireMockServer server, String query, PhotonFeatureData... features) {
        server.stubFor(get(urlPathEqualTo("/api"))
                .withQueryParam("q", equalTo(query))
                .willReturn(okJson(buildFeatureCollection(features))));
    }

    public static void stubSearchEmpty(WireMockServer server, String query) {
        server.stubFor(get(urlPathEqualTo("/api"))
                .withQueryParam("q", equalTo(query))
                .willReturn(okJson("""
                        { "features": [] }
                        """)));
    }

    public static void stubSearchFails(WireMockServer server, String query, int status) {
        server.stubFor(get(urlPathEqualTo("/api"))
                .withQueryParam("q", equalTo(query))
                .willReturn(aResponse().withStatus(status)));
    }

    public static void stubReverseEmpty(WireMockServer server) {
        server.stubFor(get(urlPathEqualTo("/reverse"))
                .willReturn(okJson("""
                        { "features": [] }
                        """)));
    }

    /**
     * A Photon feature built for stubs. All fields populated — partial features
     * silently break dedup (see class Javadoc).
     */
    public record PhotonFeatureData(
            long osmId,
            String osmType,
            String osmKey,
            String osmValue,
            String type,
            String name,
            String country,
            String state,
            String city,
            String postcode,
            String countrycode,
            double lon,
            double lat
    ) {
        public static PhotonFeatureData krakow() {
            return new PhotonFeatureData(
                    Constants.OSM_ID_KRAKOW, "R", "place", "city", "city",
                    Constants.LOCATION_NAME_KRAKOW, "Poland", "Lesser Poland",
                    Constants.LOCATION_NAME_KRAKOW, "30-001", "PL",
                    Constants.LON_KRAKOW, Constants.LAT_KRAKOW);
        }

        public static PhotonFeatureData warsaw() {
            return new PhotonFeatureData(
                    Constants.OSM_ID_WARSAW, "R", "place", "city", "city",
                    Constants.LOCATION_NAME_WARSAW, "Poland", "Mazovia",
                    Constants.LOCATION_NAME_WARSAW, "00-001", "PL",
                    Constants.LON_WARSAW, Constants.LAT_WARSAW);
        }
    }

    private static String buildFeatureCollection(PhotonFeatureData... features) {
        StringBuilder sb = new StringBuilder("{\"features\":[");
        for (int i = 0; i < features.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(toJson(features[i]));
        }
        sb.append("]}");
        return sb.toString();
    }

    private static String toJson(PhotonFeatureData f) {
        return """
                {
                  "type": "Feature",
                  "geometry": {"type": "Point", "coordinates": [%s, %s]},
                  "properties": {
                    "osm_id": %d,
                    "osm_type": "%s",
                    "osm_key": "%s",
                    "osm_value": "%s",
                    "type": "%s",
                    "countrycode": "%s",
                    "name": "%s",
                    "country": "%s",
                    "state": "%s",
                    "city": "%s",
                    "postcode": "%s"
                  }
                }
                """.formatted(
                f.lon, f.lat,
                f.osmId, f.osmType, f.osmKey, f.osmValue, f.type, f.countrycode,
                f.name, f.country, f.state, f.city, f.postcode);
    }
}
