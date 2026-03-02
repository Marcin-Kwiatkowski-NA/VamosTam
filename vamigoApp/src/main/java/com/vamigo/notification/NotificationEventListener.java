package com.vamigo.notification;

import com.vamigo.messaging.Conversation;
import com.vamigo.messaging.ConversationRepository;
import com.vamigo.messaging.MessageRepository;
import com.vamigo.messaging.MessageType;
import com.vamigo.messaging.event.MessageCreatedEvent;
import com.vamigo.ride.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Dedicated listener that converts domain events into persistent notifications.
 * Each handler builds a {@link NotificationRequest} and delegates to
 * {@link NotificationService#notify(NotificationRequest)} which persists,
 * broadcasts via STOMP, and dispatches push.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final NotificationParamsEnricher enricher;

    public NotificationEventListener(NotificationService notificationService,
                                     ConversationRepository conversationRepository,
                                     MessageRepository messageRepository,
                                     NotificationParamsEnricher enricher) {
        this.notificationService = notificationService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.enricher = enricher;
    }

    // -- Booking events --

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingRequested(BookingRequestedEvent event) {
        notifyBooking(event.driverId(), event.rideId(), event.bookingId(),
                NotificationType.BOOKING_REQUESTED, event.driverId(), event.passengerId(), event.driverId(), null);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        notifyBooking(event.passengerId(), event.rideId(), event.bookingId(),
                NotificationType.BOOKING_CONFIRMED, event.passengerId(), event.driverId(), event.driverId(), null);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingRejected(BookingRejectedEvent event) {
        notifyBooking(event.passengerId(), event.rideId(), event.bookingId(),
                NotificationType.BOOKING_REJECTED, event.passengerId(), event.driverId(), event.driverId(), null);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledEvent event) {
        Long recipientId = event.cancelledByUserId().equals(event.driverId())
                ? event.passengerId()
                : event.driverId();
        Long counterpartyId = event.cancelledByUserId();
        try {
            var enriched = enricher.enrichBooking(event.bookingId(), event.rideId(), counterpartyId,
                    NotificationType.BOOKING_CANCELLED, recipientId, event.driverId(), event.reason());
            notificationService.notify(NotificationRequest.builder()
                    .recipientId(recipientId)
                    .type(NotificationType.BOOKING_CANCELLED)
                    .entityType(EntityType.RIDE)
                    .entityId(event.rideId().toString())
                    .params(enriched.toMap())
                    .collapseKey("booking:" + event.bookingId())
                    .build());
        } catch (Exception e) {
            log.error("Failed to notify booking {} (type=BOOKING_CANCELLED): {}",
                    event.bookingId(), e.getMessage(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingExpired(BookingExpiredEvent event) {
        notifyBooking(event.passengerId(), event.rideId(), event.bookingId(),
                NotificationType.BOOKING_EXPIRED, event.passengerId(), event.driverId(), event.driverId(), null);
    }

    // -- Message events --

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreated(MessageCreatedEvent event) {
        try {
            // Skip system messages
            var message = messageRepository.findById(event.messageId()).orElse(null);
            if (message == null || message.getMessageType() == MessageType.SYSTEM) {
                return;
            }

            Conversation conversation = conversationRepository
                    .findByIdWithParticipants(event.conversationId())
                    .orElse(null);
            if (conversation == null) {
                log.warn("Conversation {} not found for notification", event.conversationId());
                return;
            }

            Long recipientId = conversation.getParticipantA().getId().equals(event.senderId())
                    ? conversation.getParticipantB().getId()
                    : conversation.getParticipantA().getId();

            var enriched = enricher.enrichChat(conversation, event.senderId());

            notificationService.notify(NotificationRequest.builder()
                    .recipientId(recipientId)
                    .type(NotificationType.CHAT_MESSAGE_NEW)
                    .entityType(EntityType.CONVERSATION)
                    .entityId(event.conversationId().toString())
                    .params(enriched.toMap())
                    .collapseKey("conv:" + event.conversationId())
                    .build());

        } catch (Exception e) {
            log.error("Failed to notify for message {}: {}", event.messageId(), e.getMessage(), e);
        }
    }

    // -- Private helpers --

    private void notifyBooking(Long recipientId, Long rideId, Long bookingId,
                                NotificationType type, Long recipient, Long counterpartyId,
                                Long driverId, String reason) {
        try {
            var enriched = enricher.enrichBooking(bookingId, rideId, counterpartyId,
                    type, recipientId, driverId, reason);
            notificationService.notify(NotificationRequest.builder()
                    .recipientId(recipientId)
                    .type(type)
                    .entityType(EntityType.RIDE)
                    .entityId(rideId.toString())
                    .params(enriched.toMap())
                    .collapseKey("booking:" + bookingId)
                    .build());
        } catch (Exception e) {
            log.error("Failed to notify booking {} (type={}): {}", bookingId, type, e.getMessage(), e);
        }
    }
}
