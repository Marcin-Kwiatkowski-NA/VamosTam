package com.vamigo.notification;

import com.vamigo.domain.PersonDisplayNameResolver;
import com.vamigo.location.Location;
import com.vamigo.messaging.Conversation;
import com.vamigo.ride.Ride;
import com.vamigo.ride.RideBooking;
import com.vamigo.ride.RideBookingRepository;
import com.vamigo.ride.RideRepository;
import com.vamigo.ride.RideStop;
import com.vamigo.searchalert.SavedSearch;
import com.vamigo.user.UserProfile;
import com.vamigo.user.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationParamsEnricherTest {

    @Mock private RideBookingRepository bookingRepository;
    @Mock private RideRepository rideRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private PersonDisplayNameResolver displayNameResolver;
    @InjectMocks private NotificationParamsEnricher enricher;

    private Location krakowLocation;
    private Location warsawLocation;

    @BeforeEach
    void setUp() {
        krakowLocation = Location.builder().namePl("Kraków").nameEn("Krakow").build();
        warsawLocation = Location.builder().namePl("Warszawa").nameEn("Warsaw").build();
    }

    private Ride rideWithStops(Long rideId) {
        var originStop = RideStop.builder().stopOrder(0).location(krakowLocation).build();
        var destStop = RideStop.builder().stopOrder(1).location(warsawLocation).build();
        return Ride.builder().id(rideId).stops(List.of(originStop, destStop)).build();
    }

    private RideBooking bookingWithRide(Long bookingId, Ride ride) {
        return RideBooking.builder().id(bookingId).ride(ride).build();
    }

    private void mockCounterpartyName(Long userId, String name) {
        var profile = UserProfile.builder().displayName(name).build();
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(displayNameResolver.resolveInternal(profile, userId)).thenReturn(name);
    }

    @Nested
    @DisplayName("Enrich notification params for a booking event")
    class EnrichBooking {

        @Test
        @DisplayName("Populates route, counterparty name, and driver-facing deep link when recipient is the driver")
        void enrichesBookingForDriver() {
            Ride ride = rideWithStops(42L);
            RideBooking booking = bookingWithRide(10L, ride);
            when(bookingRepository.findByIdWithRideAndLocations(10L)).thenReturn(Optional.of(booking));
            mockCounterpartyName(2L, "Jan");

            var result = enricher.enrichBooking(10L, 42L, 2L,
                    NotificationType.BOOKING_REQUESTED, 1L, 1L, null);

            assertEquals("r-42", result.offerKey());
            assertEquals("Krakow", result.origin());
            assertEquals("Warsaw", result.destination());
            assertEquals("Jan", result.counterpartyName());
            assertEquals("/my-offer/r-42", result.deepLink());
            assertEquals("10", result.bookingId());
            assertNull(result.reason());
        }

        @Test
        @DisplayName("Uses the passenger-facing /offer deep link when the recipient is a passenger")
        void deepLinkForPassenger() {
            Ride ride = rideWithStops(42L);
            RideBooking booking = bookingWithRide(10L, ride);
            when(bookingRepository.findByIdWithRideAndLocations(10L)).thenReturn(Optional.of(booking));
            mockCounterpartyName(1L, "Driver");

            var result = enricher.enrichBooking(10L, 42L, 1L,
                    NotificationType.BOOKING_CONFIRMED, 2L, 1L, null);

            assertEquals("/offer/r-42", result.deepLink());
        }

        @Test
        @DisplayName("Truncates cancellation reason to 100 characters and appends an ellipsis")
        void truncatesReason() {
            when(bookingRepository.findByIdWithRideAndLocations(10L)).thenReturn(Optional.empty());

            String longReason = "a".repeat(150);
            var result = enricher.enrichBooking(10L, 42L, 2L,
                    NotificationType.BOOKING_CANCELLED, 1L, 1L, longReason);

            assertNotNull(result.reason());
            assertTrue(result.reason().length() <= 100);
            assertTrue(result.reason().endsWith("..."));
        }

        @Test
        @DisplayName("toMap emits only the fields that are populated and drops nulls")
        void toMapExcludesNulls() {
            when(bookingRepository.findByIdWithRideAndLocations(10L)).thenReturn(Optional.empty());

            var result = enricher.enrichBooking(10L, 42L, 2L,
                    NotificationType.BOOKING_REQUESTED, 1L, 1L, null);
            Map<String, String> map = result.toMap();

            assertTrue(map.containsKey("offerKey"));
            assertTrue(map.containsKey("deepLink"));
            assertFalse(map.containsKey("reason"));
            assertFalse(map.containsKey("conversationId"));
        }
    }

    @Nested
    @DisplayName("Enrich notification params for a completed ride")
    class EnrichRideCompleted {

        @Test
        @DisplayName("Populates origin, destination, offer key, and deep link from the ride's stops")
        void enrichesRideCompleted() {
            Ride ride = rideWithStops(42L);
            when(rideRepository.findByIdWithStopsAndLocations(42L)).thenReturn(Optional.of(ride));

            var result = enricher.enrichRideCompleted(42L);

            assertEquals("r-42", result.offerKey());
            assertEquals("Krakow", result.origin());
            assertEquals("Warsaw", result.destination());
            assertEquals("/offer/r-42", result.deepLink());
        }

        @Test
        @DisplayName("Falls back to offer key and deep link with null city names when the ride cannot be loaded")
        void fallsBackWhenRideNotFound() {
            when(rideRepository.findByIdWithStopsAndLocations(42L)).thenReturn(Optional.empty());

            var result = enricher.enrichRideCompleted(42L);

            assertEquals("r-42", result.offerKey());
            assertNull(result.origin());
            assertNull(result.destination());
            assertEquals("/offer/r-42", result.deepLink());
        }
    }

    @Nested
    @DisplayName("Enrich notification params for a chat message")
    class EnrichChat {

        @Test
        @DisplayName("Resolves sender name, route from the conversation's offer key, and deep link to the chat")
        void enrichesChat() {
            var conversation = Conversation.builder()
                    .id(UUID.randomUUID())
                    .topicKey("offer:r-42")
                    .offerKey("r-42")
                    .build();

            Ride ride = rideWithStops(42L);
            when(rideRepository.findByIdWithStopsAndLocations(42L)).thenReturn(Optional.of(ride));
            mockCounterpartyName(5L, "Anna");

            var result = enricher.enrichChat(conversation, 5L);

            assertEquals("r-42", result.offerKey());
            assertEquals("Krakow", result.origin());
            assertEquals("Warsaw", result.destination());
            assertEquals("Anna", result.senderName());
            assertEquals("/chat/" + conversation.getId(), result.deepLink());
            assertEquals(conversation.getId().toString(), result.conversationId());
        }
    }

    @Nested
    @DisplayName("Enrich notification params for a review reminder")
    class EnrichReviewReminder {

        @Test
        @DisplayName("Populates route, offer deep link, and the counterpart-submitted flag")
        void enrichesReviewReminder() {
            Ride ride = rideWithStops(42L);
            when(rideRepository.findByIdWithStopsAndLocations(42L)).thenReturn(Optional.of(ride));

            var result = enricher.enrichReviewReminder(42L, 10L, "true");

            assertEquals("r-42", result.offerKey());
            assertEquals("Krakow", result.origin());
            assertEquals("Warsaw", result.destination());
            assertEquals("/offer/r-42", result.deepLink());
            assertEquals("10", result.bookingId());
            assertEquals("true", result.counterpartSubmitted());
        }
    }

    @Nested
    @DisplayName("Enrich notification params for a received review")
    class EnrichReviewReceived {

        @Test
        @DisplayName("Builds a deep link pointing at the review subject's reviews page")
        void enrichesReviewReceived() {
            var result = enricher.enrichReviewReceived(99L, 5L);

            assertEquals("/user/5/reviews", result.deepLink());
            assertNull(result.offerKey());
        }
    }

    @Nested
    @DisplayName("Enrich SearchAlert match — route labels with offsets")
    class EnrichSearchAlert {

        private NotificationParamsEnricher localEnricher;
        private SavedSearch savedSearch;

        @BeforeEach
        void initEnricher() {
            localEnricher = new NotificationParamsEnricher(
                    bookingRepository, rideRepository, userProfileRepository,
                    displayNameResolver, JsonMapper.builder().build());
            savedSearch = SavedSearch.builder()
                    .originName("Krak\u00f3w")
                    .destinationName("Warszawa")
                    .departureDate(LocalDate.of(2026, 4, 18))
                    .build();
        }

        @Test
        @DisplayName("composeSideLabel: nearby side renders stop name with ceil km offset")
        void labelNearby() {
            assertEquals("Wieliczka +6 km",
                    localEnricher.composeSideLabel("Wieliczka", 5400, "Krak\u00f3w"));
        }

        @Test
        @DisplayName("composeSideLabel: distance at or under 500 m folds to saved-search name")
        void labelFoldThreshold() {
            assertEquals("Krak\u00f3w",
                    localEnricher.composeSideLabel("Wieliczka", 500, "Krak\u00f3w"));
            assertEquals("Krak\u00f3w",
                    localEnricher.composeSideLabel("Wieliczka", 0, "Krak\u00f3w"));
        }

        @Test
        @DisplayName("composeSideLabel: null stop / null distance falls back to saved-search name")
        void labelNullFallback() {
            assertEquals("Krak\u00f3w",
                    localEnricher.composeSideLabel(null, null, "Krak\u00f3w"));
            assertEquals("Krak\u00f3w",
                    localEnricher.composeSideLabel(null, 5000, "Krak\u00f3w"));
        }

        @Test
        @DisplayName("enrichSearchAlertSingle emits originLabel / destinationLabel with mixed sides")
        void singleMixedSides() {
            var info = new NotificationParamsEnricher.SearchAlertMatchInfo(
                    Instant.parse("2026-04-18T06:30:00Z"),
                    false,
                    "Wieliczka", 5400,
                    null, null,
                    new BigDecimal("45"));

            Map<String, String> rich = localEnricher.enrichSearchAlertSingle(savedSearch, info);

            assertEquals("Wieliczka +6 km", rich.get("originLabel"));
            assertEquals("Warszawa", rich.get("destinationLabel"));
            assertEquals("45", rich.get("minPrice"));
            assertNotNull(rich.get("previewRows"));
            assertFalse(rich.containsKey("driverName"));
        }

        @Test
        @DisplayName("enrichSearchAlertMulti emits exactCount / nearbyCount")
        void multiCounts() {
            var exact = new NotificationParamsEnricher.SearchAlertMatchInfo(
                    Instant.parse("2026-04-18T06:30:00Z"),
                    true, null, null, null, null, new BigDecimal("40"));
            var nearby1 = new NotificationParamsEnricher.SearchAlertMatchInfo(
                    Instant.parse("2026-04-18T08:00:00Z"),
                    false, "Wieliczka", 5400, "Pr\u00f3szk\u00f3w", 14200,
                    new BigDecimal("50"));
            var nearby2 = new NotificationParamsEnricher.SearchAlertMatchInfo(
                    Instant.parse("2026-04-18T10:00:00Z"),
                    false, "Skawina", 3800, null, null,
                    new BigDecimal("55"));

            Map<String, String> rich = localEnricher.enrichSearchAlertMulti(
                    savedSearch, List.of(exact, nearby1, nearby2));

            assertEquals("1", rich.get("exactCount"));
            assertEquals("2", rich.get("nearbyCount"));
            assertEquals("40", rich.get("minPrice"));
            assertNotNull(rich.get("previewRows"));
        }
    }

    @Nested
    @DisplayName("Static helpers")
    class StaticHelpers {

        @Test
        @DisplayName("truncateCity clips city names longer than 25 characters and appends an ellipsis")
        void truncatesLongCity() {
            String longCity = "a".repeat(30);
            String result = NotificationParamsEnricher.truncateCity(longCity);
            assertEquals(25, result.length());
            assertTrue(result.endsWith("..."));
        }

        @Test
        @DisplayName("truncateCity leaves short city names unchanged")
        void keepsShortCity() {
            assertEquals("Kraków", NotificationParamsEnricher.truncateCity("Kraków"));
        }

        @Test
        @DisplayName("routeLabel formats origin and destination as \"origin → destination\"")
        void routeLabel() {
            assertEquals("Krakow \u2192 Warsaw",
                    NotificationParamsEnricher.routeLabel("Krakow", "Warsaw"));
        }

        @Test
        @DisplayName("routeLabel returns null when either origin or destination is missing")
        void routeLabelNullWhenIncomplete() {
            assertNull(NotificationParamsEnricher.routeLabel(null, "Warsaw"));
            assertNull(NotificationParamsEnricher.routeLabel("Krakow", null));
        }
    }
}
