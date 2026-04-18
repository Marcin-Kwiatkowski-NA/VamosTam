package com.vamigo.notification;

import com.vamigo.domain.PersonDisplayNameResolver;
import com.vamigo.messaging.Conversation;
import com.vamigo.ride.Ride;
import com.vamigo.ride.RideBooking;
import com.vamigo.ride.RideBookingRepository;
import com.vamigo.ride.RideRepository;
import com.vamigo.user.UserProfile;
import com.vamigo.user.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Centralizes notification param enrichment — builds typed {@link EnrichedParams}
 * with route labels, counterparty names, and deep links for each notification type.
 */
@Service
public class NotificationParamsEnricher {

    private static final Logger log = LoggerFactory.getLogger(NotificationParamsEnricher.class);
    private static final int MAX_CITY_LENGTH = 25;
    private static final int MAX_REASON_LENGTH = 100;
    /** Per-side cap so 3 rows with two names + two offsets stay under the 256-char bigBody cap. */
    private static final int MAX_PREVIEW_NAME_LENGTH = 18;
    private static final int MAX_PREVIEW_ROWS = 3;
    private static final int MAX_BIG_BODY_LENGTH = 256;
    /** Render preview times in the platform's default zone — Europe/Warsaw in production. */
    private static final ZoneId PREVIEW_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT_EN = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);

    private final RideBookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserProfileRepository userProfileRepository;
    private final PersonDisplayNameResolver displayNameResolver;
    private final JsonMapper jsonMapper;

    public NotificationParamsEnricher(RideBookingRepository bookingRepository,
                                       RideRepository rideRepository,
                                       UserProfileRepository userProfileRepository,
                                       PersonDisplayNameResolver displayNameResolver,
                                       JsonMapper jsonMapper) {
        this.bookingRepository = bookingRepository;
        this.rideRepository = rideRepository;
        this.userProfileRepository = userProfileRepository;
        this.displayNameResolver = displayNameResolver;
        this.jsonMapper = jsonMapper;
    }

    public record EnrichedParams(
            String offerKey,
            String origin,
            String destination,
            String counterpartyName,
            String deepLink,
            String conversationId,
            String senderName,
            String bookingId,
            String reason,
            String counterpartSubmitted
    ) {
        public Map<String, String> toMap() {
            var map = new LinkedHashMap<String, String>();
            putIfNotNull(map, "offerKey", offerKey);
            putIfNotNull(map, "origin", origin);
            putIfNotNull(map, "destination", destination);
            putIfNotNull(map, "counterpartyName", counterpartyName);
            putIfNotNull(map, "deepLink", deepLink);
            putIfNotNull(map, "conversationId", conversationId);
            putIfNotNull(map, "senderName", senderName);
            putIfNotNull(map, "bookingId", bookingId);
            putIfNotNull(map, "reason", reason);
            putIfNotNull(map, "counterpartSubmitted", counterpartSubmitted);
            return map;
        }

        private static void putIfNotNull(Map<String, String> map, String key, String value) {
            if (value != null) map.put(key, value);
        }
    }

    /**
     * Enrich booking notifications with ride route, counterparty name, and deep link.
     */
    public EnrichedParams enrichBooking(Long bookingId, Long rideId, Long counterpartyId,
                                         NotificationType type, Long recipientId, Long driverId,
                                         String reason) {
        String offerKey = "r-" + rideId;
        String origin = null;
        String destination = null;
        String counterpartyName = null;

        try {
            RideBooking booking = bookingRepository.findByIdWithRideAndLocations(bookingId).orElse(null);
            if (booking != null) {
                Ride ride = booking.getRide();
                origin = truncateCity(ride.getOrigin().getName(null));
                destination = truncateCity(ride.getDestination().getName(null));
            }
        } catch (Exception e) {
            log.warn("Failed to resolve ride route for booking {}: {}", bookingId, e.getMessage());
        }

        try {
            counterpartyName = resolveDisplayName(counterpartyId);
        } catch (Exception e) {
            log.warn("Failed to resolve counterparty name for user {}: {}", counterpartyId, e.getMessage());
        }

        boolean isDriverRecipient = recipientId.equals(driverId);
        String deepLink = switch (type) {
            case BOOKING_REQUESTED -> "/my-offer/" + offerKey;
            case BOOKING_CONFIRMED, BOOKING_REJECTED, BOOKING_EXPIRED -> "/offer/" + offerKey;
            case BOOKING_CANCELLED -> isDriverRecipient ? "/my-offer/" + offerKey : "/offer/" + offerKey;
            default -> "/offer/" + offerKey;
        };

        String safeReason = truncateReason(reason);

        return new EnrichedParams(offerKey, origin, destination, counterpartyName, deepLink,
                null, null, bookingId.toString(), safeReason, null);
    }

    /**
     * Enrich ride-completed notifications with route.
     */
    public EnrichedParams enrichRideCompleted(Long rideId) {
        String offerKey = "r-" + rideId;
        String origin = null;
        String destination = null;

        try {
            Ride ride = rideRepository.findByIdWithStopsAndLocations(rideId).orElse(null);
            if (ride != null) {
                origin = truncateCity(ride.getOrigin().getName(null));
                destination = truncateCity(ride.getDestination().getName(null));
            }
        } catch (Exception e) {
            log.warn("Failed to resolve ride route for ride {}: {}", rideId, e.getMessage());
        }

        String deepLink = "/offer/" + offerKey;
        return new EnrichedParams(offerKey, origin, destination, null, deepLink,
                null, null, null, null, null);
    }

    /**
     * Enrich chat message notifications with sender name and ride context from conversation's offerKey.
     */
    public EnrichedParams enrichChat(Conversation conversation, Long senderId) {
        String conversationId = conversation.getId().toString();
        String senderName = null;
        String offerKey = conversation.getOfferKey();
        String origin = null;
        String destination = null;

        try {
            senderName = resolveDisplayName(senderId);
        } catch (Exception e) {
            log.warn("Failed to resolve sender name for user {}: {}", senderId, e.getMessage());
        }

        if (offerKey != null && offerKey.startsWith("r-")) {
            try {
                Long rideId = Long.parseLong(offerKey.substring(2));
                Ride ride = rideRepository.findByIdWithStopsAndLocations(rideId).orElse(null);
                if (ride != null) {
                    origin = truncateCity(ride.getOrigin().getName(null));
                    destination = truncateCity(ride.getDestination().getName(null));
                }
            } catch (Exception e) {
                log.warn("Failed to resolve ride route for conversation {}: {}", conversationId, e.getMessage());
            }
        }

        String deepLink = "/chat/" + conversationId;
        return new EnrichedParams(offerKey, origin, destination, null, deepLink,
                conversationId, senderName, null, null, null);
    }

    /**
     * Enrich review reminder with ride route.
     */
    public EnrichedParams enrichReviewReminder(Long rideId, Long bookingId, String counterpartSubmitted) {
        String offerKey = "r-" + rideId;
        String origin = null;
        String destination = null;

        try {
            Ride ride = rideRepository.findByIdWithStopsAndLocations(rideId).orElse(null);
            if (ride != null) {
                origin = truncateCity(ride.getOrigin().getName(null));
                destination = truncateCity(ride.getDestination().getName(null));
            }
        } catch (Exception e) {
            log.warn("Failed to resolve ride route for review reminder, ride {}: {}", rideId, e.getMessage());
        }

        String deepLink = "/offer/" + offerKey;
        return new EnrichedParams(offerKey, origin, destination, null, deepLink,
                null, null, bookingId != null ? bookingId.toString() : null,
                null, counterpartSubmitted);
    }

    /**
     * Enrich review-received notification with deep link to subject's reviews page.
     */
    public EnrichedParams enrichReviewReceived(Long reviewId, Long subjectId) {
        String deepLink = "/user/" + subjectId + "/reviews";
        return new EnrichedParams(null, null, null, null, deepLink,
                null, null, null, null, null);
    }

    /**
     * Build the typed {@code listFilters} map + derived compat {@code deepLink}
     * for a {@code SEARCH_ALERT_MATCH} LIST push. The scheduler decides the
     * {@code resultKind} (RIDE vs SEAT) and the route prefix.
     */
    public SearchAlertListFilters buildSearchAlertListFilters(com.vamigo.searchalert.SavedSearch ss,
                                                              String listRoutePrefix) {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("originOsmId", ss.getOriginOsmId());
        filters.put("destinationOsmId", ss.getDestinationOsmId());
        filters.put("originName", truncateCity(ss.getOriginName()));
        filters.put("destinationName", truncateCity(ss.getDestinationName()));
        filters.put("originLat", ss.getOriginLat());
        filters.put("originLon", ss.getOriginLon());
        filters.put("destinationLat", ss.getDestinationLat());
        filters.put("destinationLon", ss.getDestinationLon());
        filters.put("earliestDeparture", ss.getDepartureDate().format(DateTimeFormatter.ISO_LOCAL_DATE));

        String deepLink = listRoutePrefix
                + "?originOsmId=" + ss.getOriginOsmId()
                + "&destinationOsmId=" + ss.getDestinationOsmId()
                + "&originName=" + ss.getOriginName()
                + "&destinationName=" + ss.getDestinationName()
                + "&originLat=" + ss.getOriginLat()
                + "&originLon=" + ss.getOriginLon()
                + "&destinationLat=" + ss.getDestinationLat()
                + "&destinationLon=" + ss.getDestinationLon()
                + "&earliestDeparture=" + ss.getDepartureDate();

        return new SearchAlertListFilters(filters, deepLink);
    }

    /**
     * Deep link for a single-match SearchAlert (ENTITY target). Detail routes
     * are owned by the frontend resolver; this string is a compat fallback only.
     */
    public String buildSearchAlertEntityDeepLink(ResultKind kind, long id) {
        return kind == ResultKind.SEAT ? "/seats/offer/s-" + id : "/rides/offer/r-" + id;
    }

    public record SearchAlertListFilters(Map<String, Object> filters, String deepLink) { }

    /**
     * Compact summary of a single match used to render the rich SearchAlert
     * push notification — caller supplies these from the persisted
     * {@link com.vamigo.searchalert.SearchAlertMatch} row plus Ride/Seat rows
     * (for departureTime + price). Stop-name/distance fields are {@code null}
     * when that side is exact — the renderer falls back to the saved-search
     * city name with no offset suffix.
     */
    public record SearchAlertMatchInfo(
            java.time.Instant departureTime,
            boolean exactMatch,
            String rideOriginStopName,
            Integer originDistanceM,
            String rideDestinationStopName,
            Integer destinationDistanceM,
            BigDecimal price
    ) { }

    /** Distances at or below this value count as exact on that side. */
    private static final int EXACT_SIDE_THRESHOLD_METERS = 500;

    /**
     * Build the rich-content keys for a single-match SearchAlert push.
     * Adds {@code departureDateFmt} (title) and body params via
     * {@code earliestDeparture}, {@code originLabel}, {@code destinationLabel},
     * {@code minPrice}, and {@code previewRows}.
     */
    public Map<String, String> enrichSearchAlertSingle(com.vamigo.searchalert.SavedSearch ss,
                                                        SearchAlertMatchInfo match) {
        Map<String, String> rich = new LinkedHashMap<>();
        rich.put("departureDateFmt", formatDate(ss.getDepartureDate()));
        if (match == null) return rich;

        String time = formatTime(match.departureTime());
        if (time != null) rich.put("earliestDeparture", time);

        String price = formatPrice(match.price());
        if (price != null) rich.put("minPrice", price);

        rich.put("originLabel", composeSideLabel(
                match.rideOriginStopName(), match.originDistanceM(),
                ss.getOriginName()));
        rich.put("destinationLabel", composeSideLabel(
                match.rideDestinationStopName(), match.destinationDistanceM(),
                ss.getDestinationName()));

        String previewJson = encodePreviewRows(ss, List.of(match));
        if (previewJson != null) rich.put("previewRows", previewJson);

        return rich;
    }

    /**
     * Build the rich-content keys for a multi-match SearchAlert push.
     * Caller passes the matches sorted by departure ascending; we cap at
     * {@link #MAX_PREVIEW_ROWS} and derive {@code earliestDeparture} +
     * {@code minPrice} + {@code exactCount}/{@code nearbyCount} across the
     * whole set.
     */
    public Map<String, String> enrichSearchAlertMulti(com.vamigo.searchalert.SavedSearch ss,
                                                       List<SearchAlertMatchInfo> matches) {
        Map<String, String> rich = new LinkedHashMap<>();
        rich.put("departureDateFmt", formatDate(ss.getDepartureDate()));
        if (matches == null || matches.isEmpty()) return rich;

        String earliest = matches.stream()
                .map(SearchAlertMatchInfo::departureTime)
                .filter(java.util.Objects::nonNull)
                .min(java.time.Instant::compareTo)
                .map(this::formatTime)
                .orElse(null);
        if (earliest != null) rich.put("earliestDeparture", earliest);

        BigDecimal min = matches.stream()
                .map(SearchAlertMatchInfo::price)
                .filter(java.util.Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);
        String minPrice = formatPrice(min);
        if (minPrice != null) rich.put("minPrice", minPrice);

        long exactCount = matches.stream().filter(SearchAlertMatchInfo::exactMatch).count();
        rich.put("exactCount", Long.toString(exactCount));
        rich.put("nearbyCount", Long.toString(matches.size() - exactCount));

        List<SearchAlertMatchInfo> previewRows = matches.size() > MAX_PREVIEW_ROWS
                ? matches.subList(0, MAX_PREVIEW_ROWS)
                : matches;
        String previewJson = encodePreviewRows(ss, previewRows);
        if (previewJson != null) rich.put("previewRows", previewJson);

        return rich;
    }

    /**
     * Compose the per-side label used in body + preview rows. Returns the
     * saved-search city name (e.g. {@code "Kraków"}) when the side is exact
     * or within {@link #EXACT_SIDE_THRESHOLD_METERS}. Otherwise returns
     * {@code "<stopName> +<km> km"} with ceil-rounded km.
     */
    public String composeSideLabel(String rideStopName, Integer distanceM, String savedSearchName) {
        String fallback = truncatePreviewName(savedSearchName);
        if (rideStopName == null || distanceM == null) {
            return fallback != null ? fallback : "";
        }
        if (distanceM <= EXACT_SIDE_THRESHOLD_METERS) {
            return fallback != null ? fallback : "";
        }
        String stop = truncatePreviewName(rideStopName);
        if (stop == null) return fallback != null ? fallback : "";
        long km = (long) Math.ceil(distanceM / 1000.0);
        return stop + " +" + km + " km";
    }

    private String encodePreviewRows(com.vamigo.searchalert.SavedSearch ss,
                                     List<SearchAlertMatchInfo> matches) {
        var rows = new ArrayList<Map<String, String>>(matches.size());
        for (var m : matches) {
            var row = new LinkedHashMap<String, String>();
            String t = formatTime(m.departureTime());
            if (t != null) row.put("time", t);
            row.put("originLabel", composeSideLabel(
                    m.rideOriginStopName(), m.originDistanceM(), ss.getOriginName()));
            row.put("destinationLabel", composeSideLabel(
                    m.rideDestinationStopName(), m.destinationDistanceM(), ss.getDestinationName()));
            String p = formatPrice(m.price());
            if (p != null) row.put("price", p);
            if (!row.isEmpty()) rows.add(row);
        }
        if (rows.isEmpty()) return null;
        try {
            return jsonMapper.writeValueAsString(rows);
        } catch (JacksonException e) {
            log.warn("Failed to encode SearchAlert previewRows: {}", e.getMessage());
            return null;
        }
    }

    private String formatTime(java.time.Instant when) {
        if (when == null) return null;
        return ZonedDateTime.ofInstant(when, PREVIEW_ZONE).format(TIME_FMT);
    }

    private String formatDate(LocalDate date) {
        return date == null ? null : date.format(DATE_FMT_EN);
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) return null;
        return price.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static String truncatePreviewName(String name) {
        if (name == null || name.isBlank()) return null;
        return name.length() > MAX_PREVIEW_NAME_LENGTH
                ? name.substring(0, MAX_PREVIEW_NAME_LENGTH - 1) + "\u2026"
                : name;
    }

    /**
     * Cap rendered bigBody at {@link #MAX_BIG_BODY_LENGTH} so we stay below
     * the FCM 4 KB data-payload ceiling even with other rich fields present.
     */
    public static String capBigBody(String bigBody) {
        if (bigBody == null || bigBody.length() <= MAX_BIG_BODY_LENGTH) return bigBody;
        return bigBody.substring(0, MAX_BIG_BODY_LENGTH - 1) + "\u2026";
    }

    // -- Helpers --

    private String resolveDisplayName(Long userId) {
        UserProfile profile = userProfileRepository.findById(userId).orElse(null);
        return displayNameResolver.resolveInternal(profile, userId);
    }

    static String truncateCity(String city) {
        if (city == null) return null;
        return city.length() > MAX_CITY_LENGTH
                ? city.substring(0, MAX_CITY_LENGTH - 3) + "..."
                : city;
    }

    static String truncateReason(String reason) {
        if (reason == null || reason.isBlank()) return null;
        return reason.length() > MAX_REASON_LENGTH
                ? reason.substring(0, MAX_REASON_LENGTH - 3) + "..."
                : reason;
    }

    /**
     * Build a route label "{origin} → {destination}" or null if data is missing.
     */
    public static String routeLabel(String origin, String destination) {
        if (origin != null && destination != null) {
            return origin + " \u2192 " + destination;
        }
        return null;
    }
}
