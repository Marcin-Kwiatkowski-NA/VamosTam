package com.vamigo.map.dto;

import java.util.List;

public record RoutePreviewResponse(
        boolean routeAvailable,
        GeoJsonLineString route,
        GeoJsonFeatureCollection stops,
        double[] bbox,
        CameraPadding cameraPadding,
        List<LegSummary> legs,
        RouteSummary summary
) {

    public record GeoJsonLineString(String type, List<List<Double>> coordinates) {
        public GeoJsonLineString(List<List<Double>> coordinates) {
            this("LineString", coordinates);
        }
    }

    public record GeoJsonFeatureCollection(String type, List<GeoJsonFeature> features) {
        public GeoJsonFeatureCollection(List<GeoJsonFeature> features) {
            this("FeatureCollection", features);
        }
    }

    public record GeoJsonFeature(String type, String id, GeoJsonPoint geometry, StopProperties properties) {
        public GeoJsonFeature(String id, GeoJsonPoint geometry, StopProperties properties) {
            this("Feature", id, geometry, properties);
        }
    }

    public record GeoJsonPoint(String type, List<Double> coordinates) {
        public GeoJsonPoint(double lon, double lat) {
            this("Point", List.of(lon, lat));
        }
    }

    public record StopProperties(String id, int stopOrder, String name, String role) {}

    public record CameraPadding(int top, int bottom, int left, int right) {}

    public record LegSummary(double distanceMeters, double durationSeconds) {}

    public record RouteSummary(double totalDistanceMeters, double totalDurationSeconds, int stopCount) {}
}
