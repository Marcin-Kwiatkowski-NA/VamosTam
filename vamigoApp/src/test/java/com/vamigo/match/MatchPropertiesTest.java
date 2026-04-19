package com.vamigo.match;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MatchPropertiesTest {

    private static final GeoPoint A = new GeoPoint(50.0, 20.0);
    private static final GeoPoint B = new GeoPoint(52.0, 21.0);

    @Test
    @DisplayName("fixed-km entry produces a Fixed strategy with the given kilometers")
    void fixedKmBuildsFixedStrategy() {
        MatchProperties.FeatureRadius fr = new MatchProperties.FeatureRadius(
                50.0, null, null, null, null);

        RadiusStrategy strategy = fr.toStrategy();

        assertThat(strategy).isInstanceOf(RadiusStrategy.Fixed.class);
        assertThat(strategy.resolveMeters(A, B)).isEqualTo(50_000);
    }

    @Test
    @DisplayName("divisor + min-km entry produces a Dynamic strategy")
    void dynamicEntryBuildsDynamicStrategy() {
        MatchProperties.FeatureRadius fr = new MatchProperties.FeatureRadius(
                null, 3.0, 30.0, null, null);

        RadiusStrategy strategy = fr.toStrategy();

        assertThat(strategy).isInstanceOf(RadiusStrategy.Dynamic.class);
        // same-point → minKm clamp → 30km in meters
        assertThat(strategy.resolveMeters(A, A)).isEqualTo(30_000);
    }

    @Test
    @DisplayName("dynamic entry honours max-km when provided")
    void dynamicEntryRespectsMaxKm() {
        MatchProperties.FeatureRadius fr = new MatchProperties.FeatureRadius(
                null, 1.0, 10.0, 100.0, null);

        RadiusStrategy strategy = fr.toStrategy();

        // Krakow→Warsaw ≈ 252 km, divisor=1 → 252 km, capped at 100 km.
        GeoPoint krakow = new GeoPoint(50.0647, 19.9450);
        GeoPoint warsaw = new GeoPoint(52.2297, 21.0122);
        assertThat(strategy.resolveMeters(krakow, warsaw)).isEqualTo(100_000);
    }

    @Test
    @DisplayName("fixed-km wins over divisor/min-km when both are provided")
    void fixedKmTakesPriority() {
        MatchProperties.FeatureRadius fr = new MatchProperties.FeatureRadius(
                25.0, 3.0, 30.0, null, null);

        RadiusStrategy strategy = fr.toStrategy();

        assertThat(strategy).isInstanceOf(RadiusStrategy.Fixed.class);
        assertThat(strategy.resolveMeters(A, B)).isEqualTo(25_000);
    }

    @Test
    @DisplayName("Neither fixed-km nor dynamic config is a startup error")
    void missingConfigThrows() {
        MatchProperties.FeatureRadius emptyConfig = new MatchProperties.FeatureRadius(
                null, null, null, null, null);
        MatchProperties.FeatureRadius divisorOnly = new MatchProperties.FeatureRadius(
                null, 3.0, null, null, null);
        MatchProperties.FeatureRadius minKmOnly = new MatchProperties.FeatureRadius(
                null, null, 30.0, null, null);

        assertThatThrownBy(emptyConfig::toStrategy).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(divisorOnly::toStrategy).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(minKmOnly::toStrategy).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Top-level accessors surface each feature's strategy")
    void topLevelAccessorsReturnPerFeatureStrategy() {
        MatchProperties props = new MatchProperties(
                new MatchProperties.FeatureRadius(null, 3.0, 30.0, null, null),   // rideSmart
                new MatchProperties.FeatureRadius(null, 4.0, 20.0, null, null),   // seatSmart
                new MatchProperties.FeatureRadius(50.0, null, null, null, null),  // alert
                new MatchProperties.FeatureRadius(75.0, null, null, null, null)   // externalImport
        );

        assertThat(props.rideSmartRadius()).isInstanceOf(RadiusStrategy.Dynamic.class);
        assertThat(props.seatSmartRadius()).isInstanceOf(RadiusStrategy.Dynamic.class);
        assertThat(props.alertRadius().resolveMeters(A, B)).isEqualTo(50_000);
        assertThat(props.externalImportRadius().resolveMeters(A, B)).isEqualTo(75_000);
    }

    @Test
    @DisplayName("short-trip override uses the short radius when distance is below the threshold")
    void shortTripOverrideBelowThreshold() {
        MatchProperties.FeatureRadius fr = new MatchProperties.FeatureRadius(
                50.0, null, null, null,
                new MatchProperties.ShortTrip(150.0, 20.0));

        RadiusStrategy strategy = fr.toStrategy();

        assertThat(strategy).isInstanceOf(RadiusStrategy.ShortTripOverride.class);
        // Same point → 0 km ≪ 150 km → short radius wins.
        assertThat(strategy.resolveMeters(A, A)).isEqualTo(20_000);
    }

    @Test
    @DisplayName("short-trip override falls through to base when distance is above the threshold")
    void shortTripOverrideAboveThreshold() {
        MatchProperties.FeatureRadius fr = new MatchProperties.FeatureRadius(
                50.0, null, null, null,
                new MatchProperties.ShortTrip(150.0, 20.0));

        RadiusStrategy strategy = fr.toStrategy();

        // Krakow→Warsaw ≈ 252 km ≫ 150 km → base fixed-km wins.
        GeoPoint krakow = new GeoPoint(50.0647, 19.9450);
        GeoPoint warsaw = new GeoPoint(52.2297, 21.0122);
        assertThat(strategy.resolveMeters(krakow, warsaw)).isEqualTo(50_000);
    }

    @Test
    @DisplayName("short-trip override also wraps a Dynamic base when present")
    void shortTripOverrideWrapsDynamicBase() {
        MatchProperties.FeatureRadius fr = new MatchProperties.FeatureRadius(
                null, 3.0, 30.0, null,
                new MatchProperties.ShortTrip(150.0, 20.0));

        RadiusStrategy strategy = fr.toStrategy();

        assertThat(strategy).isInstanceOf(RadiusStrategy.ShortTripOverride.class);
        // Krakow→Warsaw ≈ 252 km → above threshold → Dynamic: 252/3 ≈ 84 km.
        GeoPoint krakow = new GeoPoint(50.0647, 19.9450);
        GeoPoint warsaw = new GeoPoint(52.2297, 21.0122);
        double expectedKm = com.vamigo.search.GeoUtils.haversineKm(
                krakow.lat(), krakow.lon(), warsaw.lat(), warsaw.lon()) / 3.0;
        assertThat(strategy.resolveMeters(krakow, warsaw)).isEqualTo(expectedKm * 1000);
        // Same-point → 0 km → short radius wins.
        assertThat(strategy.resolveMeters(A, A)).isEqualTo(20_000);
    }

    @Test
    @DisplayName("short-trip with only one of threshold-km / radius-km fails fast")
    void shortTripPartialConfigThrows() {
        MatchProperties.FeatureRadius missingRadius = new MatchProperties.FeatureRadius(
                50.0, null, null, null,
                new MatchProperties.ShortTrip(150.0, null));
        MatchProperties.FeatureRadius missingThreshold = new MatchProperties.FeatureRadius(
                50.0, null, null, null,
                new MatchProperties.ShortTrip(null, 20.0));

        assertThatThrownBy(missingRadius::toStrategy).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(missingThreshold::toStrategy).isInstanceOf(IllegalStateException.class);
    }
}
