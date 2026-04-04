package com.vamigo.map.osrm;

import java.util.List;

public record OsrmRouteResponse(String code, List<OsrmRoute> routes) {

    public record OsrmRoute(
            OsrmGeometry geometry,
            double distance,
            double duration,
            List<OsrmLeg> legs
    ) {}

    public record OsrmGeometry(String type, List<List<Double>> coordinates) {}

    public record OsrmLeg(double distance, double duration) {}
}
