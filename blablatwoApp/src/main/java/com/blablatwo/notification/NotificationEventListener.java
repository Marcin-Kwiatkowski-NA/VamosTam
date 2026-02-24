package com.blablatwo.notification;

import com.blablatwo.messaging.ConversationRepository;
import com.blablatwo.messaging.MessageRepository;
import com.blablatwo.messaging.MessageType;
import com.blablatwo.messaging.event.MessageCreatedEvent;
import com.blablatwo.ride.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

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

    public NotificationEventListener(NotificationService notificationService,
                                     ConversationRepository conversationRepository,
                                     MessageRepository messageRepository) {
        this.notificationService = notificationService;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
    }

    // -- Booking events --

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingRequested(BookingRequestedEvent event) {
        notifyBooking(event.driverId(), event.rideId(), event.bookingId(), NotificationType.BOOKING_REQUESTED);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingConfirmed(BookingConfirmedEvent event) {
        notifyBooking(event.passengerId(), event.rideId(), event.bookingId(), NotificationType.BOOKING_CONFIRMED);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingRejected(BookingRejectedEvent event) {
        notifyBooking(event.passengerId(), event.rideId(), event.bookingId(), NotificationType.BOOKING_REJECTED);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCancelled(BookingCancelledEvent event) {
        Long recipientId = event.cancelledByUserId().equals(event.driverId())
                ? event.passengerId()
                : event.driverId();
        try {
            var params = new java.util.HashMap<>(Map.of("offerKey", "r-" + event.rideId()));
            if (event.reason() != null && !event.reason().isBlank()) {
                params.put("reason", event.reason());
            }
            notificationService.notify(NotificationRequest.builder()
                    .recipientId(recipientId)
                    .type(NotificationType.BOOKING_CANCELLED)
                    .entityType(EntityType.RIDE)
                    .entityId(event.rideId().toString())
                    .params(params)
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
        notifyBooking(event.passengerId(), event.rideId(), event.bookingId(), NotificationType.BOOKING_EXPIRED);
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

            var conversation = conversationRepository
                    .findByIdWithParticipants(event.conversationId())
                    .orElse(null);
            if (conversation == null) {
                log.warn("Conversation {} not found for notification", event.conversationId());
                return;
            }

            Long recipientId = conversation.getParticipantA().getId().equals(event.senderId())
                    ? conversation.getParticipantB().getId()
                    : conversation.getParticipantA().getId();

            notificationService.notify(NotificationRequest.builder()
                    .recipientId(recipientId)
                    .type(NotificationType.CHAT_MESSAGE_NEW)
                    .entityType(EntityType.CONVERSATION)
                    .entityId(event.conversationId().toString())
                    .params(Map.of("conversationId", event.conversationId().toString()))
                    .collapseKey("conv:" + event.conversationId())
                    .build());

        } catch (Exception e) {
            log.error("Failed to notify for message {}: {}", event.messageId(), e.getMessage(), e);
        }
    }

    // -- Private helpers --

    private void notifyBooking(Long recipientId, Long rideId, Long bookingId, NotificationType type) {
        try {
            notificationService.notify(NotificationRequest.builder()
                    .recipientId(recipientId)
                    .type(type)
                    .entityType(EntityType.RIDE)
                    .entityId(rideId.toString())
                    .params(Map.of("offerKey", "r-" + rideId))
                    .collapseKey("booking:" + bookingId)
                    .build());
        } catch (Exception e) {
            log.error("Failed to notify booking {} (type={}): {}", bookingId, type, e.getMessage(), e);
        }
    }
}
