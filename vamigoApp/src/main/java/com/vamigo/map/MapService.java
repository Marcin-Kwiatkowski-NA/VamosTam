package com.vamigo.map;

import com.vamigo.exceptions.NoSuchRideException;
import com.vamigo.exceptions.NoSuchSeatException;
import com.vamigo.location.Location;
import com.vamigo.map.dto.RoutePreviewRequest;
import com.vamigo.map.dto.RoutePreviewResponse;
import com.vamigo.map.dto.RoutePreviewResponse.*;
import com.vamigo.map.osrm.OsrmClient;
import com.vamigo.map.osrm.OsrmRouteResponse;
import com.vamigo.ride.Ride;
import com.vamigo.ride.RideRepository;
import com.vamigo.ride.RideStop;
import com.vamigo.seat.Seat;
import com.vamigo.seat.SeatRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MapService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MapService.class);
    private static final String DEFAULT_PROFILE = "driving";
    private static final CameraPadding DEFAULT_CAMERA_PADDING = new CameraPadding(80, 120, 50, 50);

    private final RideRepository rideRepository;
    private final SeatRepository seatRepository;
    private final OsrmClient osrmClient;

    public MapService(RideRepository rideRepository, SeatRepository seatRepository, OsrmClient osrmClient) {
        this.rideRepository = rideRepository;
        this.seatRepository = seatRepository;
        this.osrmClient = osrmClient;
    }

    @Transactional(readOnly = true)
    public RoutePreviewResponse getRoutePreviewForRide(Long rideId) {
        Ride ride = rideRepository.findByIdWithStopsAndLocations(rideId)
                .orElseThrow(() -> new NoSuchRideException(rideId));

        List<RideStop> sortedStops = ride.getStops().stream()
                .sorted(Comparator.comparingInt(RideStop::getStopOrder))
                .toList();

        List<StopInfo> stopInfos = new ArrayList<>();
        List<double[]> coordinates = new ArrayList<>();

        for (RideStop stop : sortedStops) {
            Location loc = stop.getLocation();
            double lon = loc.getLongitude();
            double lat = loc.getLatitude();
            coordinates.add(new double[]{lon, lat});

            String role = stopRole(stop.getStopOrder(), sortedStops.size());
            stopInfos.add(new StopInfo(
                    "stop-" + loc.getOsmId(),
                    stop.getStopOrder(),
                    loc.getNamePl(),
                    role,
                    lon,
                    lat
            ));
        }

        return buildRoutePreview(coordinates, stopInfos);
    }

    @Transactional(readOnly = true)
    public RoutePreviewResponse getRoutePreviewForSeat(Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new NoSuchSeatException(seatId));

        Location origin = seat.getOrigin();
        Location destination = seat.getDestination();

        List<double[]> coordinates = List.of(
                new double[]{origin.getLongitude(), origin.getLatitude()},
                new double[]{destination.getLongitude(), destination.getLatitude()}
        );

        List<StopInfo> stopInfos = List.of(
                new StopInfo("stop-" + origin.getOsmId(), 0, origin.getNamePl(), "origin",
                        origin.getLongitude(), origin.getLatitude()),
                new StopInfo("stop-" + destination.getOsmId(), 1, destination.getNamePl(), "destination",
                        destination.getLongitude(), destination.getLatitude())
        );

        return buildRoutePreview(coordinates, stopInfos);
    }

    public RoutePreviewResponse getRoutePreview(RoutePreviewRequest request) {
        List<double[]> coordinates = new ArrayList<>();
        List<StopInfo> stopInfos = new ArrayList<>();

        for (int i = 0; i < request.stops().size(); i++) {
            var stop = request.stops().get(i);
            double lon = stop.longitude();
            double lat = stop.latitude();
            coordinates.add(new double[]{lon, lat});

            String role = stopRole(i, request.stops().size());
            String name = stop.name() != null ? stop.name() : "Stop " + (i + 1);
            stopInfos.add(new StopInfo("stop-" + i, i, name, role, lon, lat));
        }

        return buildRoutePreview(coordinates, stopInfos);
    }

    private RoutePreviewResponse buildRoutePreview(List<double[]> coordinates, List<StopInfo> stopInfos) {
        // Build stops GeoJSON (always populated)
        List<GeoJsonFeature> features = stopInfos.stream()
                .map(s -> new GeoJsonFeature(
                        s.id(),
                        new GeoJsonPoint(s.lon(), s.lat()),
                        new StopProperties(s.id(), s.stopOrder(), s.name(), s.role())
                ))
                .toList();
        var stops = new GeoJsonFeatureCollection(features);

        // Attempt OSRM route
        GeoJsonLineString route = null;
        List<LegSummary> legs = List.of();
        RouteSummary summary;
        double[] bbox;

        try {
            OsrmRouteResponse osrmResponse = osrmClient.route(coordinates, DEFAULT_PROFILE);

            if (osrmResponse != null && "Ok".equals(osrmResponse.code())
                    && osrmResponse.routes() != null && !osrmResponse.routes().isEmpty()) {

                var firstRoute = osrmResponse.routes().getFirst();
                route = new GeoJsonLineString(firstRoute.geometry().coordinates());

                legs = firstRoute.legs() != null
                        ? firstRoute.legs().stream()
                            .map(l -> new LegSummary(l.distance(), l.duration()))
                            .toList()
                        : List.of();

                summary = new RouteSummary(
                        firstRoute.distance(),
                        firstRoute.duration(),
                        stopInfos.size()
                );

                // Bbox from route geometry (routes curve beyond stop rectangle)
                bbox = computeBboxFromCoordinates(firstRoute.geometry().coordinates());
            } else {
                LOGGER.warn("OSRM returned no usable route (code={})", osrmResponse != null ? osrmResponse.code() : "null");
                summary = new RouteSummary(0, 0, stopInfos.size());
                bbox = computeBboxFromStops(coordinates);
            }
        } catch (RoutingException e) {
            LOGGER.warn("OSRM unavailable, returning stops-only preview: {}", e.getMessage());
            summary = new RouteSummary(0, 0, stopInfos.size());
            bbox = computeBboxFromStops(coordinates);
        }

        return new RoutePreviewResponse(
                route != null,
                route,
                stops,
                bbox,
                DEFAULT_CAMERA_PADDING,
                legs,
                summary
        );
    }

    private static double[] computeBboxFromCoordinates(List<List<Double>> coords) {
        double minLon = Double.MAX_VALUE, minLat = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;

        for (List<Double> coord : coords) {
            double lon = coord.get(0);
            double lat = coord.get(1);
            minLon = Math.min(minLon, lon);
            minLat = Math.min(minLat, lat);
            maxLon = Math.max(maxLon, lon);
            maxLat = Math.max(maxLat, lat);
        }

        return new double[]{minLon, minLat, maxLon, maxLat};
    }

    private static double[] computeBboxFromStops(List<double[]> coordinates) {
        double minLon = Double.MAX_VALUE, minLat = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE, maxLat = -Double.MAX_VALUE;

        for (double[] coord : coordinates) {
            minLon = Math.min(minLon, coord[0]);
            minLat = Math.min(minLat, coord[1]);
            maxLon = Math.max(maxLon, coord[0]);
            maxLat = Math.max(maxLat, coord[1]);
        }

        return new double[]{minLon, minLat, maxLon, maxLat};
    }

    private static String stopRole(int index, int total) {
        if (index == 0) return "origin";
        if (index == total - 1) return "destination";
        return "intermediate";
    }

    private record StopInfo(String id, int stopOrder, String name, String role, double lon, double lat) {}
}
