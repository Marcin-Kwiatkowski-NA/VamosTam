package com.vamigo.messaging.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for {@link MessageCreatedEvent} after commit and delegates
 * the actual broadcast work to {@link MessageBroadcastService}.
 *
 * <p>No {@code @Transactional} here — Spring requires
 * {@code @TransactionalEventListener} methods to be either
 * {@code REQUIRES_NEW}, {@code NOT_SUPPORTED}, or unannotated.
 * The transactional session is managed by the service layer.
 */
@Component
public class MessageBroadcastListener {

    private static final Logger log = LoggerFactory.getLogger(MessageBroadcastListener.class);

    private final MessageBroadcastService broadcastService;

    public MessageBroadcastListener(MessageBroadcastService broadcastService) {
        this.broadcastService = broadcastService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageCreated(MessageCreatedEvent event) {
        try {
            broadcastService.broadcast(event);
        } catch (Exception e) {
            // Last-resort catch — the service already logs per-step failures,
            // but guard against unexpected errors to avoid poisoning the event bus.
            log.error("Unhandled error in broadcast for message={} conversation={}",
                    event.messageId(), event.conversationId(), e);
        }
    }
}