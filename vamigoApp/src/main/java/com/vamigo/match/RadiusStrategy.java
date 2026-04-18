package com.vamigo.match;

import com.vamigo.search.GeoUtils;

/**
 * Radius policy: either a fixed meter radius, or a dynamic radius
 * computed from the great-circle distance between origin and destination.
 */
public sealed interface RadiusStrategy
        permits RadiusStrategy.Fixed, RadiusStrategy.Dynamic {

    double resolveMeters(GeoPoint origin, GeoPoint destination);

    static Fixed fixed(double meters) {
        return new Fixed(meters);
    }

    static Fixed fixedKm(double km) {
        return new Fixed(km * 1000);
    }

    static Dynamic dynamic(double divisor, double minKm, double maxKm) {
        return new Dynamic(divisor, minKm, maxKm);
    }

    record Fixed(double meters) implements RadiusStrategy {
        public Fixed {
            if (meters <= 0) {
                throw new IllegalArgumentException("radius meters must be > 0");
            }
        }

        @Override
        public double resolveMeters(GeoPoint origin, GeoPoint destination) {
            return meters;
        }
    }

    /**
     * Radius = clamp(distance/divisor, minKm, maxKm) converted to meters.
     * maxKm may be {@code 0} or negative to disable the upper bound.
     */
    record Dynamic(double divisor, double minKm, double maxKm) implements RadiusStrategy {
        public Dynamic {
            if (divisor <= 0) throw new IllegalArgumentException("divisor must be > 0");
            if (minKm < 0) throw new IllegalArgumentException("minKm must be >= 0");
        }

        @Override
        public double resolveMeters(GeoPoint origin, GeoPoint destination) {
            double distKm = GeoUtils.haversineKm(
                    origin.lat(), origin.lon(),
                    destination.lat(), destination.lon());
            double radiusKm = Math.max(distKm / divisor, minKm);
            if (maxKm > 0 && radiusKm > maxKm) {
                radiusKm = maxKm;
            }
            return radiusKm * 1000;
        }
    }
}
