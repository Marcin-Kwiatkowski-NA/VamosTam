package com.vamigo.match;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Per-feature radius policy. Every matching feature declares its own named
 * strategy here so tuning is a single YAML edit and no hardcoded constants
 * remain in code.
 */
@ConfigurationProperties(prefix = "match")
public record MatchProperties(
        FeatureRadius rideSmart,
        FeatureRadius seatSmart,
        FeatureRadius alert,
        FeatureRadius externalImport
) {

    public RadiusStrategy rideSmartRadius() {
        return rideSmart.toStrategy();
    }

    public RadiusStrategy seatSmartRadius() {
        return seatSmart.toStrategy();
    }

    public RadiusStrategy alertRadius() {
        return alert.toStrategy();
    }

    public RadiusStrategy externalImportRadius() {
        return externalImport.toStrategy();
    }

    /**
     * YAML-friendly radius descriptor. Either {@code fixed-km} is set (giving
     * a constant radius) or {@code divisor} / {@code min-km} / {@code max-km}
     * are set (giving a dynamic radius). Validation happens eagerly so
     * config errors surface on startup.
     */
    public record FeatureRadius(
            Double fixedKm,
            Double divisor,
            Double minKm,
            Double maxKm
    ) {

        public RadiusStrategy toStrategy() {
            if (fixedKm != null) {
                return RadiusStrategy.fixedKm(fixedKm);
            }
            if (divisor == null || minKm == null) {
                throw new IllegalStateException(
                        "match radius must specify either fixed-km or divisor+min-km");
            }
            return RadiusStrategy.dynamic(divisor, minKm, maxKm != null ? maxKm : 0);
        }
    }
}
