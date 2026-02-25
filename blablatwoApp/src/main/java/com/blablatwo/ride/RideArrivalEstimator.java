package com.blablatwo.ride;

import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Estimates the arrival time for a ride based on stop departure times or
 * geodesic distance between origin and destination.
 */
@Service
public class RideArrivalEstimator {

    private static final double AVERAGE_SPEED_KM_H = 60.0;

    /**
     * Estimates arrival as a UTC Instant.
     *
     * <p>Priority 1: If the last stop has an explicit departure time, use it.
     * <p>Priority 2: Calculate distance-based duration from origin to destination.
     */
    public Instant estimate(Ride ride) {
        Instant fromLastStop = estimateFromLastStopTime(ride);
        if (fromLastStop != null) {
            return fromLastStop;
        }
        return estimateFromDistance(ride);
    }

    private Instant estimateFromLastStopTime(Ride ride) {
        List<RideStop> stops = ride.getStops();
        if (stops == null || stops.size() < 2) {
            return null;
        }

        RideStop lastStop = stops.get(stops.size() - 1);
        if (lastStop.getDepartureTime() != null) {
            return lastStop.getDepartureTime();
        }

        // Check second-to-last stop (last intermediate) as a fallback
        for (int i = stops.size() - 2; i >= 1; i--) {
            RideStop stop = stops.get(i);
            if (stop.getDepartureTime() != null) {
                // Add a rough estimate from this stop to destination
                double distKm = approximateDistanceKm(
                        stop.getLocation().getCoordinates(),
                        ride.getDestination().getCoordinates());
                long additionalMinutes = Math.max(15, Math.round((distKm / AVERAGE_SPEED_KM_H) * 60));
                return stop.getDepartureTime().plusSeconds(additionalMinutes * 60);
            }
        }

        return null;
    }

    private Instant estimateFromDistance(Ride ride) {
        Point origin = ride.getOrigin().getCoordinates();
        Point destination = ride.getDestination().getCoordinates();

        double distKm = approximateDistanceKm(origin, destination);
        long durationMinutes = Math.max(30, Math.round((distKm / AVERAGE_SPEED_KM_H) * 60));

        return ride.getDepartureTime().plusSeconds(durationMinutes * 60);
    }

    /**
     * Approximate distance using the equirectangular projection.
     * Accurate enough for driving distance estimates within Europe.
     */
    private double approximateDistanceKm(Point a, Point b) {
        double lat1 = Math.toRadians(a.getY());
        double lat2 = Math.toRadians(b.getY());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.getX() - a.getX());

        double x = dLon * Math.cos((lat1 + lat2) / 2);
        double distMeters = Math.sqrt(x * x + dLat * dLat) * 6_371_000;
        return distMeters / 1000.0;
    }
}
