package com.vamigo.match;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteQueryTest {

    private static final GeoPoint A = new GeoPoint(50.0, 20.0);
    private static final GeoPoint B = new GeoPoint(52.0, 21.0);

    @Test
    @DisplayName("hasExactOsmIds is true only when both osm ids are non-null")
    void hasExactOsmIdsWhenBothPresent() {
        assertThat(new RouteQuery(A, B, 1L, 2L).hasExactOsmIds()).isTrue();
    }

    @Test
    @DisplayName("hasExactOsmIds is false when origin osm id is null")
    void falseWhenOriginOsmIdMissing() {
        assertThat(new RouteQuery(A, B, null, 2L).hasExactOsmIds()).isFalse();
    }

    @Test
    @DisplayName("hasExactOsmIds is false when destination osm id is null")
    void falseWhenDestinationOsmIdMissing() {
        assertThat(new RouteQuery(A, B, 1L, null).hasExactOsmIds()).isFalse();
    }

    @Test
    @DisplayName("hasExactOsmIds is false when both osm ids are null")
    void falseWhenBothOsmIdsMissing() {
        assertThat(new RouteQuery(A, B, null, null).hasExactOsmIds()).isFalse();
    }
}
