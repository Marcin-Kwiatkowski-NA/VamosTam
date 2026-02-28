package com.blablatwo.notification;

import com.blablatwo.messaging.Conversation;
import com.blablatwo.messaging.ConversationRepository;
import com.blablatwo.messaging.Message;
import com.blablatwo.messaging.MessageRepository;
import com.blablatwo.messaging.MessageType;
import com.blablatwo.messaging.event.MessageCreatedEvent;
import com.blablatwo.ride.event.*;
import com.blablatwo.user.UserAccount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock private NotificationService notificationService;
    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private NotificationParamsEnricher enricher;
    @InjectMocks private NotificationEventListener listener;

    private static final NotificationParamsEnricher.EnrichedParams BOOKING_ENRICHED =
            new NotificationParamsEnricher.EnrichedParams(
                    "r-42", "Krakow", "Warsaw", "Jan", "/my-offer/r-42",
                    null, null, "10", null, null);

    private static final NotificationParamsEnricher.EnrichedParams CHAT_ENRICHED =
            new NotificationParamsEnricher.EnrichedParams(
                    "r-42", "Krakow", "Warsaw", null, "/chat/conv-uuid",
                    "conv-uuid", "Anna", null, null, null);

    @Nested
    @DisplayName("Booking events")
    class BookingEvents {

        @Test
        @DisplayName("onBookingRequested should notify driver with enriched params")
        void bookingRequested() {
            when(enricher.enrichBooking(eq(10L), eq(42L), eq(2L),
                    eq(NotificationType.BOOKING_REQUESTED), eq(1L), eq(1L), isNull()))
                    .thenReturn(BOOKING_ENRICHED);

            listener.onBookingRequested(new BookingRequestedEvent(10L, 42L, 2L, 1L));

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).notify(captor.capture());

            NotificationRequest req = captor.getValue();
            assertEquals(1L, req.recipientId());
            assertEquals(NotificationType.BOOKING_REQUESTED, req.type());
            assertEquals(EntityType.RIDE, req.entityType());
            assertEquals("42", req.entityId());
            assertEquals("Krakow", req.params().get("origin"));
            assertEquals("Warsaw", req.params().get("destination"));
            assertEquals("/my-offer/r-42", req.params().get("deepLink"));
            assertEquals("booking:10", req.collapseKey());
        }

        @Test
        @DisplayName("onBookingConfirmed should notify passenger with enriched params")
        void bookingConfirmed() {
            var enriched = new NotificationParamsEnricher.EnrichedParams(
                    "r-42", "Krakow", "Warsaw", "Driver", "/offer/r-42",
                    null, null, "10", null, null);
            when(enricher.enrichBooking(eq(10L), eq(42L), eq(1L),
                    eq(NotificationType.BOOKING_CONFIRMED), eq(2L), eq(1L), isNull()))
                    .thenReturn(enriched);

            listener.onBookingConfirmed(new BookingConfirmedEvent(10L, 42L, 2L, 1L));

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).notify(captor.capture());

            assertEquals(2L, captor.getValue().recipientId());
            assertEquals(NotificationType.BOOKING_CONFIRMED, captor.getValue().type());
            assertEquals("/offer/r-42", captor.getValue().params().get("deepLink"));
        }

        @Test
        @DisplayName("onBookingCancelled by driver should notify passenger with reason")
        void bookingCancelledByDriver() {
            var enriched = new NotificationParamsEnricher.EnrichedParams(
                    "r-42", "Krakow", "Warsaw", "Driver", "/offer/r-42",
                    null, null, "10", "changed plans", null);
            when(enricher.enrichBooking(eq(10L), eq(42L), eq(1L),
                    eq(NotificationType.BOOKING_CANCELLED), eq(2L), eq(1L), eq("changed plans")))
                    .thenReturn(enriched);

            listener.onBookingCancelled(new BookingCancelledEvent(10L, 42L, 2L, 1L, 1L, "changed plans"));

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).notify(captor.capture());

            assertEquals(2L, captor.getValue().recipientId());
            assertEquals("changed plans", captor.getValue().params().get("reason"));
        }

        @Test
        @DisplayName("onBookingCancelled by passenger should notify driver")
        void bookingCancelledByPassenger() {
            var enriched = new NotificationParamsEnricher.EnrichedParams(
                    "r-42", null, null, "Passenger", "/my-offer/r-42",
                    null, null, "10", null, null);
            when(enricher.enrichBooking(eq(10L), eq(42L), eq(2L),
                    eq(NotificationType.BOOKING_CANCELLED), eq(1L), eq(1L), isNull()))
                    .thenReturn(enriched);

            listener.onBookingCancelled(new BookingCancelledEvent(10L, 42L, 2L, 1L, 2L, null));

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).notify(captor.capture());

            assertEquals(1L, captor.getValue().recipientId());
        }

        @Test
        @DisplayName("onBookingExpired should notify passenger")
        void bookingExpired() {
            var enriched = new NotificationParamsEnricher.EnrichedParams(
                    "r-42", null, null, null, "/offer/r-42",
                    null, null, "10", null, null);
            when(enricher.enrichBooking(eq(10L), eq(42L), eq(1L),
                    eq(NotificationType.BOOKING_EXPIRED), eq(2L), eq(1L), isNull()))
                    .thenReturn(enriched);

            listener.onBookingExpired(new BookingExpiredEvent(10L, 42L, 2L, 1L));

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).notify(captor.capture());

            assertEquals(2L, captor.getValue().recipientId());
            assertEquals(NotificationType.BOOKING_EXPIRED, captor.getValue().type());
        }
    }

    @Nested
    @DisplayName("Message events")
    class MessageEvents {

        private final UUID conversationId = UUID.randomUUID();
        private final UUID messageId = UUID.randomUUID();

        @Test
        @DisplayName("should notify non-sender participant with enriched chat params")
        void notifiesNonSender() {
            UserAccount participantA = UserAccount.builder().build();
            participantA.setId(1L);
            UserAccount participantB = UserAccount.builder().build();
            participantB.setId(2L);

            Message message = Message.builder().messageType(MessageType.USER).body("hello").build();
            Conversation conversation = mock(Conversation.class);
            when(conversation.getParticipantA()).thenReturn(participantA);
            when(conversation.getParticipantB()).thenReturn(participantB);

            when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));
            when(conversationRepository.findByIdWithParticipants(conversationId))
                    .thenReturn(Optional.of(conversation));
            when(enricher.enrichChat(conversation, 1L)).thenReturn(CHAT_ENRICHED);

            listener.onMessageCreated(new MessageCreatedEvent(messageId, conversationId, 1L, Instant.now()));

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).notify(captor.capture());

            NotificationRequest req = captor.getValue();
            assertEquals(2L, req.recipientId());
            assertEquals(NotificationType.CHAT_MESSAGE_NEW, req.type());
            assertEquals(EntityType.CONVERSATION, req.entityType());
            assertEquals("Anna", req.params().get("senderName"));
            assertNotNull(req.params().get("deepLink"));
        }

        @Test
        @DisplayName("should skip system messages")
        void skipsSystemMessages() {
            Message message = Message.builder().messageType(MessageType.SYSTEM).body("system.booking_confirmed").build();
            when(messageRepository.findById(messageId)).thenReturn(Optional.of(message));

            listener.onMessageCreated(new MessageCreatedEvent(messageId, conversationId, 1L, Instant.now()));

            verify(notificationService, never()).notify(any());
        }

        @Test
        @DisplayName("should skip if message not found")
        void skipsUnknownMessage() {
            when(messageRepository.findById(messageId)).thenReturn(Optional.empty());

            listener.onMessageCreated(new MessageCreatedEvent(messageId, conversationId, 1L, Instant.now()));

            verify(notificationService, never()).notify(any());
        }
    }
}
