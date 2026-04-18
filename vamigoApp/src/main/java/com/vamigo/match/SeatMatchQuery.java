package com.vamigo.match;

public record SeatMatchQuery(
        RouteQuery route,
        RadiusStrategy radius,
        DateWindow window,
        MatchFilters filters
) {

    public SeatMatchQuery {
        if (route == null) throw new IllegalArgumentException("route required");
        if (radius == null) throw new IllegalArgumentException("radius required");
        if (window == null) window = DateWindow.openEnded(null);
        if (filters == null) filters = MatchFilters.none();
    }
}
