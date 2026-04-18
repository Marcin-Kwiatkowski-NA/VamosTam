package com.vamigo.match;

import com.vamigo.search.GeoUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RadiusStrategyTest {

    private static final GeoPoint KRAKOW = new GeoPoint(50.0647, 19.9450);
    private static final GeoPoint WARSAW = new GeoPoint(52.2297, 21.0122);

    @Nested
    @DisplayName("Fixed radius")
    class FixedTests {

        @Test
        @DisplayName("fixed(meters) returns the given meters regardless of endpoints")
        void resolvesToConfiguredMeters() {
            RadiusStrategy strategy = RadiusStrategy.fixed(1_234);

            assertThat(strategy.resolveMeters(KRAKOW, WARSAW)).isEqualTo(1_234);
            assertThat(strategy.resolveMeters(KRAKOW, KRAKOW)).isEqualTo(1_234);
        }

        @Test
        @DisplayName("fixedKm multiplies by 1000")
        void fixedKmConvertsToMeters() {
            assertThat(RadiusStrategy.fixedKm(50).resolveMeters(KRAKOW, WARSAW))
                    .isEqualTo(50_000);
        }

        @Test
        @DisplayName("meters <= 0 is rejected at construction")
        void rejectsNonPositiveMeters() {
            assertThrows(IllegalArgumentException.class, () -> RadiusStrategy.fixed(0));
            assertThrows(IllegalArgumentException.class, () -> RadiusStrategy.fixed(-1));
        }
    }

    @Nested
    @DisplayName("Dynamic radius")
    class DynamicTests {

        @Test
        @DisplayName("Returns distance/divisor in meters when above minKm")
        void usesDistanceOverDivisor() {
            double distKm = GeoUtils.haversineKm(
                    KRAKOW.lat(), KRAKOW.lon(), WARSAW.lat(), WARSAW.lon());
            // Krakow→Warsaw ≈ 252km. 252/3 ≈ 84km (above min of 30km).
            RadiusStrategy strategy = RadiusStrategy.dynamic(3, 30, 0);

            double resolved = strategy.resolveMeters(KRAKOW, WARSAW);

            assertThat(resolved).isEqualTo(distKm / 3 * 1000);
        }

        @Test
        @DisplayName("Clamps up to minKm when distance/divisor is tiny")
        void clampsToMinimum() {
            // Same point → distance 0 → without clamp radius would be 0.
            RadiusStrategy strategy = RadiusStrategy.dynamic(3, 30, 0);

            assertThat(strategy.resolveMeters(KRAKOW, KRAKOW)).isEqualTo(30_000);
        }

        @Test
        @DisplayName("Clamps down to maxKm when distance/divisor exceeds it")
        void clampsToMaximum() {
            // distance ≈ 252km, divisor=1 → 252km. Cap at 100km.
            RadiusStrategy strategy = RadiusStrategy.dynamic(1, 30, 100);

            assertThat(strategy.resolveMeters(KRAKOW, WARSAW)).isEqualTo(100_000);
        }

        @ParameterizedTest(name = "maxKm={0} disables upper bound")
        @CsvSource({"0", "-1"})
        @DisplayName("maxKm <= 0 disables the upper bound")
        void nonPositiveMaxDisablesClamp(double maxKm) {
            double distKm = GeoUtils.haversineKm(
                    KRAKOW.lat(), KRAKOW.lon(), WARSAW.lat(), WARSAW.lon());
            RadiusStrategy strategy = RadiusStrategy.dynamic(1, 30, maxKm);

            assertThat(strategy.resolveMeters(KRAKOW, WARSAW)).isEqualTo(distKm * 1000);
        }

        @Test
        @DisplayName("divisor <= 0 is rejected at construction")
        void rejectsNonPositiveDivisor() {
            assertThrows(IllegalArgumentException.class, () -> RadiusStrategy.dynamic(0, 30, 0));
            assertThrows(IllegalArgumentException.class, () -> RadiusStrategy.dynamic(-1, 30, 0));
        }

        @Test
        @DisplayName("minKm < 0 is rejected at construction")
        void rejectsNegativeMinKm() {
            assertThrows(IllegalArgumentException.class, () -> RadiusStrategy.dynamic(3, -1, 0));
        }
    }
}
