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

import java.util.LinkedHashMap;
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

    private final RideBookingRepository bookingRepository;
    private final RideRepository rideRepository;
    private final UserProfileRepository userProfileRepository;
    private final PersonDisplayNameResolver displayNameResolver;

    public NotificationParamsEnricher(RideBookingRepository bookingRepository,
                                       RideRepository rideRepository,
                                       UserProfileRepository userProfileRepository,
                                       PersonDisplayNameResolver displayNameResolver) {
        this.bookingRepository = bookingRepository;
        this.rideRepository = rideRepository;
        this.userProfileRepository = userProfileRepository;
        this.displayNameResolver = displayNameResolver;
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
