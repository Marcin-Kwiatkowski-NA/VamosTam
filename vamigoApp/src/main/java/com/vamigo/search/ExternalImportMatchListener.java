package com.vamigo.search;

import com.vamigo.email.BrevoClient;
import com.vamigo.email.EmailSendException;
import com.vamigo.location.LocationDto;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.ride.dto.RideSearchCriteriaDto;
import com.vamigo.ride.event.ExternalRideCreatedEvent;
import com.vamigo.ride.RideService;
import com.vamigo.seat.SeatService;
import com.vamigo.seat.dto.SeatResponseDto;
import com.vamigo.seat.dto.SeatSearchCriteriaDto;
import com.vamigo.seat.event.ExternalSeatCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class ExternalImportMatchListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalImportMatchListener.class);
    private static final int MAX_RESULTS = 20;
    private static final double RADIUS_KM = 50.0;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneOffset.UTC);

    private final SeatService seatService;
    private final RideService rideService;
    private final BrevoClient brevoClient;
    private final String notifyAddress;
    private final String senderAddress;
    private final String senderName;
    private final String frontendUrl;

    public ExternalImportMatchListener(SeatService seatService,
                                       RideService rideService,
                                       BrevoClient brevoClient,
                                       @Value("${app.email.external-import-notify-address}") String notifyAddress,
                                       @Value("${app.email.sender-address}") String senderAddress,
                                       @Value("${app.email.sender-name}") String senderName,
                                       @Value("${app.email.external-import-frontend-url}") String frontendUrl) {
        this.seatService = seatService;
        this.rideService = rideService;
        this.brevoClient = brevoClient;
        this.notifyAddress = notifyAddress;
        this.senderAddress = senderAddress;
        this.senderName = senderName;
        this.frontendUrl = frontendUrl;
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExternalRideCreated(ExternalRideCreatedEvent event) {
        RideResponseDto ride = event.ride();
        LocationDto origin = ride.origin();
        LocationDto destination = ride.destination();

        if (origin == null || destination == null
                || origin.latitude() == null || origin.longitude() == null
                || destination.latitude() == null || destination.longitude() == null) {
            LOGGER.warn("Skipping match notification for ride {} — missing coordinates", ride.id());
            return;
        }

        Instant dayStart = ride.departureTime().truncatedTo(ChronoUnit.DAYS);
        Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);

        var criteria = new SeatSearchCriteriaDto(
                null, null,
                origin.latitude(), origin.longitude(),
                destination.latitude(), destination.longitude(),
                RADIUS_KM,
                dayStart, dayEnd,
                null
        );

        Page<SeatResponseDto> matches = seatService.searchSeats(criteria, PageRequest.of(0, MAX_RESULTS));

        if (matches.isEmpty()) {
            LOGGER.debug("No matching seats found for external ride {}", ride.id());
            return;
        }

        String searchUrl = buildSearchUrl("/rides/seats",
                origin, destination, dayStart, dayEnd);

        sendEmail(
                "External ride: %s → %s — %d matching seat requests".formatted(
                        origin.name(), destination.name(), matches.getTotalElements()),
                buildRideMatchHtml(ride, event.sourceUrl(), matches.getContent(), searchUrl),
                buildRideMatchText(ride, event.sourceUrl(), matches.getContent(), searchUrl)
        );
    }

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExternalSeatCreated(ExternalSeatCreatedEvent event) {
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
                1
        );

        Page<RideResponseDto> matches = rideService.searchRides(criteria, PageRequest.of(0, MAX_RESULTS));

        if (matches.isEmpty()) {
            LOGGER.debug("No matching rides found for external seat {}", seat.id());
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
                .queryParam("destinationOsmId", destination.osmId())
                .queryParam("destinationName", destination.name())
                .queryParam("earliestDeparture", earliestDeparture.toString())
                .queryParam("latestDeparture", latestDeparture.toString())
                .build()
                .toUriString();
    }

    private String buildRideMatchHtml(RideResponseDto ride, String sourceUrl,
                                      List<SeatResponseDto> matches, String searchUrl) {
        var sb = new StringBuilder();
        sb.append("<h2>New External Ride</h2>");
        sb.append("<table style=\"border-collapse:collapse;width:100%%;\">");
        sb.append("<tr><td><strong>Origin:</strong></td><td>%s</td></tr>".formatted(esc(ride.origin().name())));
        sb.append("<tr><td><strong>Destination:</strong></td><td>%s</td></tr>".formatted(esc(ride.destination().name())));
        sb.append("<tr><td><strong>Departure:</strong></td><td>%s</td></tr>".formatted(DATE_FMT.format(ride.departureTime())));
        sb.append("<tr><td><strong>External URL:</strong></td><td><a href=\"%s\">%s</a></td></tr>".formatted(esc(sourceUrl), esc(sourceUrl)));
        sb.append("</table>");

        sb.append("<p><a href=\"%s\">View search results in app</a></p>".formatted(esc(searchUrl)));

        sb.append("<h3>Matching Seat Requests (%d)</h3>".formatted(matches.size()));
        sb.append("<table style=\"border-collapse:collapse;width:100%%;border:1px solid #ccc;\">");
        sb.append("<tr style=\"background:#f0f0f0;\">");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Origin</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Dist (km)</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Destination</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Dist (km)</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Departure</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Price</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Description</th>");
        sb.append("</tr>");

        for (SeatResponseDto seat : matches) {
            double originDist = GeoUtils.haversineKm(
                    ride.origin().latitude(), ride.origin().longitude(),
                    seat.origin().latitude(), seat.origin().longitude());
            double destDist = GeoUtils.haversineKm(
                    ride.destination().latitude(), ride.destination().longitude(),
                    seat.destination().latitude(), seat.destination().longitude());

            sb.append("<tr>");
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(esc(seat.origin().name())));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%.1f</td>".formatted(originDist));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(esc(seat.destination().name())));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%.1f</td>".formatted(destDist));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(DATE_FMT.format(seat.departureTime())));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(seat.priceWillingToPay() != null ? seat.priceWillingToPay() : "-"));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(truncate(esc(seat.description()), 80)));
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String buildRideMatchText(RideResponseDto ride, String sourceUrl,
                                      List<SeatResponseDto> matches, String searchUrl) {
        var sb = new StringBuilder();
        sb.append("New External Ride\n\n");
        sb.append("Origin: %s\n".formatted(ride.origin().name()));
        sb.append("Destination: %s\n".formatted(ride.destination().name()));
        sb.append("Departure: %s\n".formatted(DATE_FMT.format(ride.departureTime())));
        sb.append("External URL: %s\n\n".formatted(sourceUrl));
        sb.append("Search results: %s\n\n".formatted(searchUrl));
        sb.append("Matching Seat Requests (%d):\n".formatted(matches.size()));

        for (SeatResponseDto seat : matches) {
            double originDist = GeoUtils.haversineKm(
                    ride.origin().latitude(), ride.origin().longitude(),
                    seat.origin().latitude(), seat.origin().longitude());
            double destDist = GeoUtils.haversineKm(
                    ride.destination().latitude(), ride.destination().longitude(),
                    seat.destination().latitude(), seat.destination().longitude());

            sb.append("  %s (%.1f km) → %s (%.1f km) | %s | %s\n".formatted(
                    seat.origin().name(), originDist,
                    seat.destination().name(), destDist,
                    DATE_FMT.format(seat.departureTime()),
                    seat.priceWillingToPay() != null ? seat.priceWillingToPay() : "-"));
        }
        return sb.toString();
    }

    private String buildSeatMatchHtml(SeatResponseDto seat, String sourceUrl,
                                      List<RideResponseDto> matches, String searchUrl) {
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
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Origin</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Dist (km)</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Destination</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Dist (km)</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Departure</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Price</th>");
        sb.append("<th style=\"padding:4px 8px;border:1px solid #ccc;\">Description</th>");
        sb.append("</tr>");

        for (RideResponseDto ride : matches) {
            double originDist = GeoUtils.haversineKm(
                    seat.origin().latitude(), seat.origin().longitude(),
                    ride.origin().latitude(), ride.origin().longitude());
            double destDist = GeoUtils.haversineKm(
                    seat.destination().latitude(), seat.destination().longitude(),
                    ride.destination().latitude(), ride.destination().longitude());

            sb.append("<tr>");
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(esc(ride.origin().name())));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%.1f</td>".formatted(originDist));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(esc(ride.destination().name())));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%.1f</td>".formatted(destDist));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(DATE_FMT.format(ride.departureTime())));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(ride.pricePerSeat() != null ? ride.pricePerSeat() : "-"));
            sb.append("<td style=\"padding:4px 8px;border:1px solid #ccc;\">%s</td>".formatted(truncate(esc(ride.description()), 80)));
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private String buildSeatMatchText(SeatResponseDto seat, String sourceUrl,
                                      List<RideResponseDto> matches, String searchUrl) {
        var sb = new StringBuilder();
        sb.append("New External Seat Request\n\n");
        sb.append("Origin: %s\n".formatted(seat.origin().name()));
        sb.append("Destination: %s\n".formatted(seat.destination().name()));
        sb.append("Departure: %s\n".formatted(DATE_FMT.format(seat.departureTime())));
        sb.append("External URL: %s\n\n".formatted(sourceUrl));
        sb.append("Search results: %s\n\n".formatted(searchUrl));
        sb.append("Matching Ride Offers (%d):\n".formatted(matches.size()));

        for (RideResponseDto ride : matches) {
            double originDist = GeoUtils.haversineKm(
                    seat.origin().latitude(), seat.origin().longitude(),
                    ride.origin().latitude(), ride.origin().longitude());
            double destDist = GeoUtils.haversineKm(
                    seat.destination().latitude(), seat.destination().longitude(),
                    ride.destination().latitude(), ride.destination().longitude());

            sb.append("  %s (%.1f km) → %s (%.1f km) | %s | %s\n".formatted(
                    ride.origin().name(), originDist,
                    ride.destination().name(), destDist,
                    DATE_FMT.format(ride.departureTime()),
                    ride.pricePerSeat() != null ? ride.pricePerSeat() : "-"));
        }
        return sb.toString();
    }

    private void sendEmail(String subject, String html, String text) {
        try {
            brevoClient.sendHtmlEmail(
                    notifyAddress, "Vamigo Import",
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
