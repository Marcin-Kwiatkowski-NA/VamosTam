package com.vamigo.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PushMessageRendererTest {

    private PushMessageRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new PushMessageRenderer(JsonMapper.builder().build());
    }

    @Nested
    @DisplayName("BOOKING_REQUESTED")
    class BookingRequested {

        @Test
        @DisplayName("title should show route when available")
        void titleWithRoute() {
            var params = Map.of("origin", "Krakow", "destination", "Warsaw");
            String title = renderer.title(NotificationType.BOOKING_REQUESTED, params);
            assertEquals("Krakow \u2192 Warsaw", title);
        }

        @Test
        @DisplayName("title should fall back when no route")
        void titleFallback() {
            String title = renderer.title(NotificationType.BOOKING_REQUESTED, Map.of());
            assertEquals("Booking update", title);
        }

        @Test
        @DisplayName("body should include counterparty name when available")
        void bodyWithName() {
            var params = Map.of("counterpartyName", "Jan");
            String body = renderer.body(NotificationType.BOOKING_REQUESTED, params);
            assertEquals("Jan wants to join your ride", body);
        }

        @Test
        @DisplayName("body should fall back when no counterparty name")
        void bodyFallback() {
            String body = renderer.body(NotificationType.BOOKING_REQUESTED, Map.of());
            assertTrue(body.contains("wants to join"));
        }
    }

    @Nested
    @DisplayName("BOOKING_CONFIRMED")
    class BookingConfirmed {

        @Test
        @DisplayName("body should say booking confirmed")
        void body() {
            String body = renderer.body(NotificationType.BOOKING_CONFIRMED, Map.of());
            assertEquals("Your booking is confirmed!", body);
        }
    }

    @Nested
    @DisplayName("BOOKING_CANCELLED")
    class BookingCancelled {

        @Test
        @DisplayName("body should include counterparty name and reason")
        void bodyWithNameAndReason() {
            var params = Map.of("counterpartyName", "Anna", "reason", "changed plans");
            String body = renderer.body(NotificationType.BOOKING_CANCELLED, params);
            assertEquals("Anna cancelled: changed plans", body);
        }

        @Test
        @DisplayName("body should fall back without name")
        void bodyFallbackWithoutName() {
            String body = renderer.body(NotificationType.BOOKING_CANCELLED, Map.of());
            assertTrue(body.contains("cancelled"));
        }
    }

    @Nested
    @DisplayName("CHAT_MESSAGE_NEW")
    class ChatMessageNew {

        @Test
        @DisplayName("title should show sender name when available")
        void titleWithSender() {
            var params = Map.of("senderName", "Anna");
            String title = renderer.title(NotificationType.CHAT_MESSAGE_NEW, params);
            assertEquals("Anna", title);
        }

        @Test
        @DisplayName("title should fall back when no sender name")
        void titleFallback() {
            String title = renderer.title(NotificationType.CHAT_MESSAGE_NEW, Map.of());
            assertEquals("New message", title);
        }

        @Test
        @DisplayName("body should include route when available")
        void bodyWithRoute() {
            var params = Map.of("origin", "Krakow", "destination", "Warsaw");
            String body = renderer.body(NotificationType.CHAT_MESSAGE_NEW, params);
            assertTrue(body.contains("Krakow"));
            assertTrue(body.contains("Warsaw"));
        }

        @Test
        @DisplayName("body should fall back when no route")
        void bodyFallback() {
            String body = renderer.body(NotificationType.CHAT_MESSAGE_NEW, Map.of());
            assertTrue(body.contains("message"));
        }
    }

    @Nested
    @DisplayName("RIDE_COMPLETED")
    class RideCompleted {

        @Test
        @DisplayName("body should mention Vamigo branding")
        void bodyWithBranding() {
            String body = renderer.body(NotificationType.RIDE_COMPLETED, Map.of());
            assertTrue(body.contains("Vamigo"));
        }
    }

    @Nested
    @DisplayName("REVIEW_RECEIVED")
    class ReviewReceived {

        @Test
        @DisplayName("title should be Vamigo")
        void title() {
            String title = renderer.title(NotificationType.REVIEW_RECEIVED, Map.of());
            assertEquals("Vamigo", title);
        }

        @Test
        @DisplayName("body should mention new review")
        void body() {
            String body = renderer.body(NotificationType.REVIEW_RECEIVED, Map.of());
            assertTrue(body.contains("review"));
        }
    }

    @Nested
    @DisplayName("SEARCH_ALERT_MATCH")
    class SearchAlertMatch {

        @Test
        @DisplayName("single-match body shows both stops with offsets")
        void singleNearbyBoth() {
            var params = Map.of(
                    "matchCount", "1",
                    "earliestDeparture", "08:30",
                    "originLabel", "Wieliczka +6 km",
                    "destinationLabel", "Pr\u00f3szk\u00f3w +15 km",
                    "minPrice", "45");
            String body = renderer.body(NotificationType.SEARCH_ALERT_MATCH, params);
            assertEquals("08:30 \u00b7 Wieliczka +6 km \u2192 Pr\u00f3szk\u00f3w +15 km \u00b7 45 z\u0142", body);
        }

        @Test
        @DisplayName("single-match body uses saved-search city on exact side")
        void singleOriginNearbyDestExact() {
            var params = Map.of(
                    "matchCount", "1",
                    "earliestDeparture", "08:30",
                    "originLabel", "Wieliczka +6 km",
                    "destinationLabel", "Warszawa",
                    "minPrice", "45");
            String body = renderer.body(NotificationType.SEARCH_ALERT_MATCH, params);
            assertEquals("08:30 \u00b7 Wieliczka +6 km \u2192 Warszawa \u00b7 45 z\u0142", body);
        }

        @Test
        @DisplayName("single-match body renders both sides exact with saved-search cities")
        void singleFullyExact() {
            var params = Map.of(
                    "matchCount", "1",
                    "earliestDeparture", "08:30",
                    "originLabel", "Krak\u00f3w",
                    "destinationLabel", "Warszawa",
                    "minPrice", "45");
            String body = renderer.body(NotificationType.SEARCH_ALERT_MATCH, params);
            assertEquals("08:30 \u00b7 Krak\u00f3w \u2192 Warszawa \u00b7 45 z\u0142", body);
        }

        @Test
        @DisplayName("multi-match body includes exact/nearby counts")
        void multiWithCounts() {
            var params = Map.of(
                    "matchCount", "3",
                    "exactCount", "1",
                    "nearbyCount", "2",
                    "earliestDeparture", "08:30",
                    "minPrice", "40");
            String body = renderer.body(NotificationType.SEARCH_ALERT_MATCH, params);
            assertEquals("3 new rides match (1 exact, 2 nearby) \u00b7 from 40 z\u0142 \u00b7 earliest 08:30", body);
        }

        @Test
        @DisplayName("bigBody renders route-shaped preview rows from JSON")
        void bigBodyRenders() throws Exception {
            var rows = "[{\"time\":\"08:30\",\"originLabel\":\"Wieliczka +6 km\",\"destinationLabel\":\"Warszawa\",\"price\":\"45\"},"
                    + "{\"time\":\"12:00\",\"originLabel\":\"Krak\u00f3w\",\"destinationLabel\":\"Pr\u00f3szk\u00f3w +15 km\",\"price\":\"40\"}]";
            var params = Map.of("previewRows", rows);
            String bigBody = renderer.bigBody(NotificationType.SEARCH_ALERT_MATCH, params);
            assertNotNull(bigBody);
            assertTrue(bigBody.contains("Wieliczka +6 km \u2192 Warszawa"), bigBody);
            assertTrue(bigBody.contains("Krak\u00f3w \u2192 Pr\u00f3szk\u00f3w +15 km"), bigBody);
            assertTrue(bigBody.contains("45 z\u0142"), bigBody);
        }
    }

    @Nested
    @DisplayName("Null params")
    class NullParams {

        @Test
        @DisplayName("should not throw with null params")
        void handlesNullParams() {
            assertDoesNotThrow(() -> renderer.title(NotificationType.BOOKING_REQUESTED, null));
            assertDoesNotThrow(() -> renderer.body(NotificationType.BOOKING_REQUESTED, null));
        }

        @Test
        @DisplayName("should return fallback title with null params")
        void fallbackTitleWithNull() {
            String title = renderer.title(NotificationType.BOOKING_REQUESTED, null);
            assertEquals("Booking update", title);
        }
    }
}
