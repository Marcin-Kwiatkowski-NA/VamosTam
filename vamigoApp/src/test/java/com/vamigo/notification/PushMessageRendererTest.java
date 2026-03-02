package com.vamigo.notification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PushMessageRendererTest {

    private PushMessageRenderer renderer;

    @BeforeEach
    void setUp() {
        renderer = new PushMessageRenderer();
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
