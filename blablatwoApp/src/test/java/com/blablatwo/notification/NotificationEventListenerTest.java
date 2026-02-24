package com.blablatwo.notification;

import com.blablatwo.messaging.ConversationRepository;
import com.blablatwo.messaging.Message;
import com.blablatwo.messaging.MessageRepository;
import com.blablatwo.messaging.MessageType;
import com.blablatwo.messaging.Conversation;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationEventListenerTest {

    @Mock private NotificationService notificationService;
    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @InjectMocks private NotificationEventListener listener;

    @Nested
    @DisplayName("Booking events")
    class BookingEvents {

        @Test
        @DisplayName("onBookingRequested should notify driver")
        void bookingRequested() {
            listener.onBookingRequested(new BookingRequestedEvent(10L, 42L, 2L, 1L));

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).notify(captor.capture());

            NotificationRequest req = captor.getValue();
            assertEquals(1L, req.recipientId());
            assertEquals(NotificationType.BOOKING_REQUESTED, req.type());
            assertEquals(EntityType.RIDE, req.entityType());
            assertEquals("42", req.entityId());
            assertEquals("r-42", req.params().get("offerKey"));
            assertEquals("booking:10", req.collapseKey());
        }

        @Test
        @DisplayName("onBookingConfirmed should notify passenger")
        void bookingConfirmed() {
            listener.onBookingConfirmed(new BookingConfirmedEvent(10L, 42L, 2L, 1L));

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).notify(captor.capture());

            assertEquals(2L, captor.getValue().recipientId());
            assertEquals(NotificationType.BOOKING_CONFIRMED, captor.getValue().type());
        }

        @Test
        @DisplayName("onBookingRejected should notify passenger")
        void bookingRejected() {
            listener.onBookingRejected(new BookingRejectedEvent(10L, 42L, 2L, 1L));

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).notify(captor.capture());

            assertEquals(2L, captor.getValue().recipientId());
            assertEquals(NotificationType.BOOKING_REJECTED, captor.getValue().type());
        }

        @Test
        @DisplayName("onBookingCancelled should notify other party (driver cancels -> notify passenger)")
        void bookingCancelledByDriver() {
            listener.onBookingCancelled(new BookingCancelledEvent(10L, 42L, 2L, 1L, 1L, "changed plans"));

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).notify(captor.capture());

            assertEquals(2L, captor.getValue().recipientId());
            assertEquals(NotificationType.BOOKING_CANCELLED, captor.getValue().type());
        }

        @Test
        @DisplayName("onBookingCancelled should notify other party (passenger cancels -> notify driver)")
        void bookingCancelledByPassenger() {
            listener.onBookingCancelled(new BookingCancelledEvent(10L, 42L, 2L, 1L, 2L, "changed plans"));

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).notify(captor.capture());

            assertEquals(1L, captor.getValue().recipientId());
        }

        @Test
        @DisplayName("onBookingExpired should notify passenger")
        void bookingExpired() {
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
        @DisplayName("should notify non-sender participant for user messages")
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

            listener.onMessageCreated(new MessageCreatedEvent(messageId, conversationId, 1L, Instant.now()));

            ArgumentCaptor<NotificationRequest> captor = ArgumentCaptor.forClass(NotificationRequest.class);
            verify(notificationService).notify(captor.capture());

            NotificationRequest req = captor.getValue();
            assertEquals(2L, req.recipientId());
            assertEquals(NotificationType.CHAT_MESSAGE_NEW, req.type());
            assertEquals(EntityType.CONVERSATION, req.entityType());
            assertEquals(conversationId.toString(), req.entityId());
            assertEquals("conv:" + conversationId, req.collapseKey());
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
