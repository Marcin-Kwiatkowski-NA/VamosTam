package com.vamigo.search;

import com.vamigo.email.BrevoClient;
import com.vamigo.email.EmailSendException;
import com.vamigo.location.LocationDto;
import com.vamigo.ride.dto.RideListDto;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.ride.dto.RideSearchCriteriaDto;
import com.vamigo.ride.dto.RideStopDto;
import com.vamigo.ride.event.ExternalRideCreatedEvent;
import com.vamigo.ride.RideService;
import com.vamigo.seat.SeatService;
import com.vamigo.seat.dto.SeatListDto;
import com.vamigo.seat.dto.SeatResponseDto;
import com.vamigo.seat.dto.SeatSearchCriteriaDto;
import com.vamigo.seat.event.ExternalSeatCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import org.springframework.context.i18n.LocaleContextHolder;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@EnableConfigurationProperties(ExternalImportProperties.class)
public class ExternalImportMatchListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalImportMatchListener.class);
    private static final int MAX_RESULTS = 20;
    private static final double RADIUS_KM = 50.0;
    private static final String WIEN_NAME = "Wiedeń";
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneOffset.UTC);

    private final SeatService seatService;
    private final RideService rideService;
    private final BrevoClient brevoClient;
    private final ExternalImportProperties importProperties;
    private final String senderAddress;
    private final String senderName;
    private final String frontendUrl;

    public ExternalImportMatchListener(SeatService seatService,
                                       RideService rideService,
                                       BrevoClient brevoClient,
                                       ExternalImportProperties importProperties,
                                       @Value("${app.email.sender-address}") String senderAddress,
                                       @Value("${app.email.sender-name}") String senderName,
                                       @Value("${app.email.external-import-frontend-url}") String frontendUrl) {
        this.seatService = seatService;
        this.rideService = rideService;
        this.brevoClient = brevoClient;
        this.importProperties = importProperties;
        this.senderAddress = senderAddress;
        this.senderName = senderName;
        this.frontendUrl = frontendUrl;
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExternalRideCreated(ExternalRideCreatedEvent event) {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pl"));
        RideResponseDto ride = event.ride();
        List<RideStopDto> stops = ride.stops();

        if (stops == null || stops.size() < 2) {
            LOGGER.warn("Skipping match notification for ride {} — insufficient stops", ride.id());
            return;
        }

        boolean allValid = stops.stream().allMatch(s ->
                s.location() != null && s.location().latitude() != null && s.location().longitude() != null);
        if (!allValid) {
            LOGGER.warn("Skipping match notification for ride {} — stops missing coordinates", ride.id());
            return;
        }

        Instant dayStart = ride.departureTime().truncatedTo(ChronoUnit.DAYS);
        Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);

        // Search for matching seats across all stop pairs (board before alight)
        Map<Long, SeatListDto> matchMap = new LinkedHashMap<>();
        for (int i = 0; i < stops.size(); i++) {
            for (int j = i + 1; j < stops.size(); j++) {
                LocationDto boardLoc = stops.get(i).location();
                LocationDto alightLoc = stops.get(j).location();

                var criteria = new SeatSearchCriteriaDto(
                        null, null,
                        boardLoc.latitude(), boardLoc.longitude(),
                        alightLoc.latitude(), alightLoc.longitude(),
                        RADIUS_KM,
                        dayStart, dayEnd,
                        null
                );

                Page<SeatListDto> page = seatService.searchSeats(criteria, PageRequest.of(0, MAX_RESULTS));
                for (SeatListDto seat : page.getContent()) {
                    matchMap.putIfAbsent(seat.id(), seat);
                }
            }
        }

        int minRequired = involvesWien(stops) ? importProperties.minMatchingResults() : 1;
        if (matchMap.size() < minRequired) {
            LOGGER.debug("Only {} matching seats for external ride {} (min {})",
                    matchMap.size(), ride.id(), minRequired);
            return;
        }

        // Sort by combined nearest-stop distance (exact matches naturally first)
        List<SeatListDto> allMatches = new ArrayList<>(matchMap.values());
        allMatches.sort(Comparator.comparingDouble(seat ->
                nearestStopDistance(stops, seat.origin().latitude(), seat.origin().longitude())
                        + nearestStopDistance(stops, seat.destination().latitude(), seat.destination().longitude())));

        if (allMatches.size() > MAX_RESULTS) {
            allMatches = allMatches.subList(0, MAX_RESULTS);
        }

        String searchUrl = buildSearchUrl("/rides/seats",
                ride.origin(), ride.destination(), dayStart, dayEnd);

        sendEmail(
                "External ride: %s → %s — %d matching seat requests".formatted(
                        ride.origin().name(), ride.destination().name(), allMatches.size()),
                buildRideMatchHtml(ride, event.sourceUrl(), allMatches, searchUrl),
                buildRideMatchText(ride, event.sourceUrl(), allMatches, searchUrl)
        );
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExternalSeatCreated(ExternalSeatCreatedEvent event) {
        LocaleContextHolder.setLocale(Locale.forLanguageTag("pl"));
        SeatResponseDto seat = event.seat();
        LocationDto origin = seat.origin();
        LocationDto destination = seat.destination();

        if (origin == null || destination == null
                || origin.latitude() == null || origin.longitude() == null
                || destination.latitude() == null || destination.longitude() == null) {
            LOGGER.warn("Skipping match notification for seat {} — missing coordinates", seat.id());
            return;
        }

        Instant dayStart = seat.departureTime().truncatedTo(ChronoUnit.DAYS);
        Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);

        var criteria = new RideSearchCriteriaDto(
                null, null,
                origin.latitude(), origin.longitude(),
                destination.latitude(), destination.longitude(),
                RADIUS_KM,
                dayStart, dayEnd,
                1,
                null
        );

        Page<RideListDto> matches = rideService.searchRides(criteria, PageRequest.of(0, MAX_RESULTS));

        int minRequired = involvesWien(origin, destination) ? importProperties.minMatchingResults() : 1;
        if (matches.getTotalElements() < minRequired) {
            LOGGER.debug("Only {} matching rides for external seat {} (min {})",
                    matches.getTotalElements(), seat.id(), minRequired);
            return;
        }

        String searchUrl = buildSearchUrl("/rides/list",
                origin, destination, dayStart, dayEnd);

        sendEmail(
                "External seat: %s → %s — %d matching ride offers".formatted(
                        origin.name(), destination.name(), matches.getTotalElements()),
                buildSeatMatchHtml(seat, event.sourceUrl(), matches.getContent(), searchUrl),
                buildSeatMatchText(seat, event.sourceUrl(), matches.getContent(), searchUrl)
        );
    }

    private String buildSearchUrl(String path, LocationDto origin, LocationDto destination,
                                  Instant earliestDeparture, Instant latestDeparture) {
        return UriComponentsBuilder.fromUriString(frontendUrl)
                .path(path)
                .queryParam("originOsmId", origin.osmId())
                .queryParam("originName", origin.name())
                .queryParam("originLat", origin.latitude())
                .queryParam("originLon", origin.longitude())
                .queryParam("destinationOsmId", destination.osmId())
                .queryParam("destinationName", destination.name())
                .queryParam("destinationLat", destination.latitude())
                .queryParam("destinationLon", destination.longitude())
                .queryParam("earliestDeparture", earliestDeparture.toString())
                .queryParam("latestDeparture", latestDeparture.toString())
                .build()
                .toUriString();
    }

    private String buildRideMatchHtml(RideResponseDto ride, String sourceUrl,
                                      List<SeatListDto> matches, String searchUrl) {
        var sb = new StringBuilder();
        List<RideStopDto> stops = ride.stops();

        sb.append("<h2>New External Ride</h2>");
        sb.append("<table style=\"border-collapse:collapse;width:100%%;\">");
        sb.append("<tr><td><strong>Route:</strong></td><td>%s</td></tr>".formatted(
                esc(stops.stream().map(s -> s.location().name()).collect(Collectors.joining(" → ")))));
        sb.append("<tr><td><strong>Departure:</strong></td><td>%s</td></tr>".formatted(DATE_FMT.format(ride.departureTime())));
        sb.append("<tr><td><strong>External URL:</strong></td><td><a href=\"%s\">%s</a></td></tr>".formatted(esc(sourceUrl), esc(sourceUrl)));
        sb.append("</table>");

        sb.append("<p><a href=\"%s\">View search results in app</a></p>".formatted(esc(searchUrl)));

        sb.append("<h3>Matching Seat Requests (%d)</h3>".formatted(matches.size()));
        sb.append("<table style=\"border-collapse:collapse;width:100%%;border:1px solid #ccc;\">");
        sb.append("<tr style=\"background:#f0f0f0;\">");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Origin</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Board @</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Destination</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Alight @</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Departure</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Price</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Description</th>");
        sb.append("</tr>");

        for (SeatListDto seat : matches) {
            String boardLabel = nearestStopLabel(stops, seat.origin().latitude(), seat.origin().longitude());
            String alightLabel = nearestStopLabel(stops, seat.destination().latitude(), seat.destination().longitude());

            sb.append("<tr>");
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(esc(seat.origin().name())));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(esc(boardLabel)));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(esc(seat.destination().name())));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(esc(alightLabel)));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(DATE_FMT.format(seat.departureTime())));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(seat.priceWillingToPay() != null ? seat.priceWillingToPay() : "-"));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">-</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String buildRideMatchText(RideResponseDto ride, String sourceUrl,
                                      List<SeatListDto> matches, String searchUrl) {
        var sb = new StringBuilder();
        List<RideStopDto> stops = ride.stops();

        sb.append("New External Ride\n\n");
        sb.append("Route: %s\n".formatted(
                stops.stream().map(s -> s.location().name()).collect(Collectors.joining(" → "))));
        sb.append("Departure: %s\n".formatted(DATE_FMT.format(ride.departureTime())));
        sb.append("External URL: %s\n\n".formatted(sourceUrl));
        sb.append("Search results: %s\n\n".formatted(searchUrl));
        sb.append("Matching Seat Requests (%d):\n".formatted(matches.size()));

        for (SeatListDto seat : matches) {
            String boardLabel = nearestStopLabel(stops, seat.origin().latitude(), seat.origin().longitude());
            String alightLabel = nearestStopLabel(stops, seat.destination().latitude(), seat.destination().longitude());

            sb.append("  %s [board @ %s] → %s [alight @ %s] | %s | %s\n".formatted(
                    seat.origin().name(), boardLabel,
                    seat.destination().name(), alightLabel,
                    DATE_FMT.format(seat.departureTime()),
                    seat.priceWillingToPay() != null ? seat.priceWillingToPay() : "-"));
        }
        return sb.toString();
    }

    private String buildSeatMatchHtml(SeatResponseDto seat, String sourceUrl,
                                      List<RideListDto> matches, String searchUrl) {
        var sb = new StringBuilder();
        sb.append("<h2>New External Seat Request</h2>");
        sb.append("<table style=\"border-collapse:collapse;width:100%%;\">");
        sb.append("<tr><td><strong>Origin:</strong></td><td>%s</td></tr>".formatted(esc(seat.origin().name())));
        sb.append("<tr><td><strong>Destination:</strong></td><td>%s</td></tr>".formatted(esc(seat.destination().name())));
        sb.append("<tr><td><strong>Departure:</strong></td><td>%s</td></tr>".formatted(DATE_FMT.format(seat.departureTime())));
        sb.append("<tr><td><strong>External URL:</strong></td><td><a href=\"%s\">%s</a></td></tr>".formatted(esc(sourceUrl), esc(sourceUrl)));
        sb.append("</table>");

        sb.append("<p><a href=\"%s\">View search results in app</a></p>".formatted(esc(searchUrl)));

        sb.append("<h3>Matching Ride Offers (%d)</h3>".formatted(matches.size()));
        sb.append("<table style=\"border-collapse:collapse;width:100%%;border:1px solid #ccc;\">");
        sb.append("<tr style=\"background:#f0f0f0;\">");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Ride</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Board @</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Alight @</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Departure</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Price</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Description</th>");
        sb.append("</tr>");

        for (RideListDto ride : matches) {
            List<RideStopDto> stops = ride.stops();
            String boardLabel = nearestStopLabel(stops, seat.origin().latitude(), seat.origin().longitude());
            String alightLabel = nearestStopLabel(stops, seat.destination().latitude(), seat.destination().longitude());
            String route = stops.stream().map(s -> s.location().name()).collect(Collectors.joining(" → "));

            sb.append("<tr>");
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(esc(route)));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(esc(boardLabel)));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(esc(alightLabel)));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(DATE_FMT.format(ride.departureTime())));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(ride.pricePerSeat() != null ? ride.pricePerSeat() : "-"));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">-</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String buildSeatMatchText(SeatResponseDto seat, String sourceUrl,
                                      List<RideListDto> matches, String searchUrl) {
        var sb = new StringBuilder();
        sb.append("New External Seat Request\n\n");
        sb.append("Origin: %s\n".formatted(seat.origin().name()));
        sb.append("Destination: %s\n".formatted(seat.destination().name()));
        sb.append("Departure: %s\n".formatted(DATE_FMT.format(seat.departureTime())));
        sb.append("External URL: %s\n\n".formatted(sourceUrl));
        sb.append("Search results: %s\n\n".formatted(searchUrl));
        sb.append("Matching Ride Offers (%d):\n".formatted(matches.size()));

        for (RideListDto ride : matches) {
            List<RideStopDto> stops = ride.stops();
            String boardLabel = nearestStopLabel(stops, seat.origin().latitude(), seat.origin().longitude());
            String alightLabel = nearestStopLabel(stops, seat.destination().latitude(), seat.destination().longitude());
            String route = stops.stream().map(s -> s.location().name()).collect(Collectors.joining(" → "));

            sb.append("  %s [board @ %s, alight @ %s] | %s | %s\n".formatted(
                    route, boardLabel, alightLabel,
                    DATE_FMT.format(ride.departureTime()),
                    ride.pricePerSeat() != null ? ride.pricePerSeat() : "-"));
        }
        return sb.toString();
    }

    private String nearestStopLabel(List<RideStopDto> stops, double lat, double lon) {
        String bestName = null;
        double bestDist = Double.MAX_VALUE;
        for (RideStopDto stop : stops) {
            LocationDto loc = stop.location();
            if (loc == null || loc.latitude() == null) continue;
            double dist = GeoUtils.haversineKm(lat, lon, loc.latitude(), loc.longitude());
            if (dist < bestDist) {
                bestDist = dist;
                bestName = loc.name();
            }
        }
        return bestDist < 1.0 ? bestName + " (exact)" : "%s (%.0f km)".formatted(bestName, bestDist);
    }

    private double nearestStopDistance(List<RideStopDto> stops, double lat, double lon) {
        double bestDist = Double.MAX_VALUE;
        for (RideStopDto stop : stops) {
            LocationDto loc = stop.location();
            if (loc == null || loc.latitude() == null) continue;
            double dist = GeoUtils.haversineKm(lat, lon, loc.latitude(), loc.longitude());
            if (dist < bestDist) bestDist = dist;
        }
        return bestDist;
    }

    private static boolean involvesWien(List<RideStopDto> stops) {
        return stops.stream()
                .map(s -> s.location())
                .filter(loc -> loc != null && loc.name() != null)
                .anyMatch(loc -> loc.name().contains(WIEN_NAME));
    }

    private static boolean involvesWien(LocationDto origin, LocationDto destination) {
        return (origin != null && origin.name() != null && origin.name().contains(WIEN_NAME))
                || (destination != null && destination.name() != null && destination.name().contains(WIEN_NAME));
    }

    private void sendEmail(String subject, String html, String text) {
        try {
            brevoClient.sendHtmlEmail(
                    importProperties.notifyAddress(), "Vamigo Import",
                    senderAddress, senderName,
                    null,
                    subject, html, text);
        } catch (EmailSendException e) {
            LOGGER.error("Failed to send external import match notification: {}", e.getMessage());
        }
    }

    private static String esc(String value) {
        return value != null ? HtmlUtils.htmlEscape(value) : "";
    }

    private static String truncate(String value, int maxLen) {
        if (value == null || value.isEmpty()) return "-";
        return value.length() <= maxLen ? value : value.substring(0, maxLen) + "...";
    }
}
