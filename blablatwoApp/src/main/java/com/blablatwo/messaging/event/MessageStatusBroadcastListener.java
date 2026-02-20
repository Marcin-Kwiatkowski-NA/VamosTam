package com.blablatwo.messaging.event;

import com.blablatwo.messaging.dto.MessageStatusUpdateDto;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MessageStatusBroadcastListener {

    private final SimpMessagingTemplate messagingTemplate;

    public MessageStatusBroadcastListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStatusUpdated(MessageStatusUpdatedEvent event) {
        messagingTemplate.convertAndSendToUser(
                event.senderId().toString(),
                "/queue/message-status",
                new MessageStatusUpdateDto(event.conversationId(), event.newStatus())
        );
    }
}
