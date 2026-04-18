package com.vamigo.match;

public record RideMatchQuery(
        RouteQuery route,
        RadiusStrategy radius,
        DateWindow window,
        MatchFilters filters
) {

    public RideMatchQuery {
        if (route == null) throw new IllegalArgumentException("route required");
        if (radius == null) throw new IllegalArgumentException("radius required");
        if (window == null) window = DateWindow.openEnded(null);
        if (filters == null) filters = MatchFilters.none();
    }
}
