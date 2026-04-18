package com.vamigo.match;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MatchQueryValidationTest {

    private static final RouteQuery ROUTE = new RouteQuery(
            new GeoPoint(50, 20), new GeoPoint(52, 21), 1L, 2L);
    private static final RadiusStrategy RADIUS = RadiusStrategy.fixedKm(50);

    @Test
    @DisplayName("RideMatchQuery rejects null route")
    void rideQueryRejectsNullRoute() {
        assertThrows(IllegalArgumentException.class,
                () -> new RideMatchQuery(null, RADIUS, null, null));
    }

    @Test
    @DisplayName("RideMatchQuery rejects null radius")
    void rideQueryRejectsNullRadius() {
        assertThrows(IllegalArgumentException.class,
                () -> new RideMatchQuery(ROUTE, null, null, null));
    }

    @Test
    @DisplayName("RideMatchQuery defaults null window to open-ended")
    void rideQueryDefaultsWindow() {
        RideMatchQuery q = new RideMatchQuery(ROUTE, RADIUS, null, null);

        assertThat(q.window()).isNotNull();
        assertThat(q.window().earliest()).isNull();
        assertThat(q.window().latest()).isNull();
    }

    @Test
    @DisplayName("RideMatchQuery defaults null filters to none()")
    void rideQueryDefaultsFilters() {
        RideMatchQuery q = new RideMatchQuery(ROUTE, RADIUS, null, null);

        assertThat(q.filters()).isNotNull();
        assertThat(q.filters().driverIdFilter()).isNull();
        assertThat(q.filters().excludedUserId()).isNull();
        assertThat(q.filters().minAvailableSeats()).isNull();
    }

    @Test
    @DisplayName("RideMatchQuery keeps caller-provided window and filters")
    void rideQueryKeepsCallerValues() {
        DateWindow window = DateWindow.of(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-02-01T00:00:00Z"));
        MatchFilters filters = MatchFilters.forDriver(42L);

        RideMatchQuery q = new RideMatchQuery(ROUTE, RADIUS, window, filters);

        assertThat(q.window()).isSameAs(window);
        assertThat(q.filters()).isSameAs(filters);
    }

    @Test
    @DisplayName("SeatMatchQuery rejects null route / radius and defaults window / filters")
    void seatQueryValidationAndDefaults() {
        assertThrows(IllegalArgumentException.class,
                () -> new SeatMatchQuery(null, RADIUS, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new SeatMatchQuery(ROUTE, null, null, null));

        SeatMatchQuery q = new SeatMatchQuery(ROUTE, RADIUS, null, null);

        assertThat(q.window()).isNotNull();
        assertThat(q.filters()).isNotNull();
    }

    @Test
    @DisplayName("DateWindow.openEnded leaves latest null")
    void dateWindowOpenEnded() {
        Instant earliest = Instant.parse("2026-01-01T00:00:00Z");

        DateWindow window = DateWindow.openEnded(earliest);

        assertThat(window.earliest()).isEqualTo(earliest);
        assertThat(window.latest()).isNull();
    }
}
