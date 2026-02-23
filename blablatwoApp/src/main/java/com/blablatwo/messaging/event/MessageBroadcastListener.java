package com.blablatwo.messaging.event;

import com.blablatwo.messaging.Conversation;
import com.blablatwo.messaging.ConversationDtoBuilder;
import com.blablatwo.messaging.ConversationRepository;
import com.blablatwo.messaging.MessageRepository;
import com.blablatwo.messaging.MessageStatus;
import com.blablatwo.messaging.dto.MessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MessageBroadcastListener {

    private static final Logger log = LoggerFactory.getLogger(MessageBroadcastListener.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationDtoBuilder dtoBuilder;

    public MessageBroadcastListener(SimpMessagingTemplate messagingTemplate,
                                     MessageRepository messageRepository,
                                     ConversationRepository conversationRepository,
                                     ConversationDtoBuilder dtoBuilder) {
        this.messagingTemplate = messagingTemplate;
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.dtoBuilder = dtoBuilder;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true)
    public void onMessageCreated(MessageCreatedEvent event) {
        try {
            var message = messageRepository.findById(event.messageId()).orElse(null);
            if (message == null) {
                log.warn("Message {} not found for broadcast", event.messageId());
                return;
            }

            // 1. Broadcast message to conversation topic
            var messagePayload = new MessageDto(
                    message.getId(),
                    event.conversationId(),
                    event.senderId(),
                    false, // isMine is viewer-relative; clients resolve this by comparing senderId
                    message.getBody(),
                    message.getCreatedAt(),
                    MessageStatus.SENT,
                    message.getMessageType()
            );

            messagingTemplate.convertAndSend(
                    "/topic/conversation/" + event.conversationId(),
                    messagePayload
            );

            // 2. Send inbox updates to each participant
            // Use findByIdWithParticipants to eagerly load lazy associations —
            // this @Async method runs outside the original request session.
            var conversation = conversationRepository.findByIdWithParticipants(event.conversationId())
                    .orElse(null);
            if (conversation == null) return;

            sendInboxUpdate(conversation, conversation.getParticipantA().getId());
            sendInboxUpdate(conversation, conversation.getParticipantB().getId());

        } catch (Exception e) {
            log.error("Failed to broadcast message {}: {}", event.messageId(), e.getMessage(), e);
        }
    }

    private void sendInboxUpdate(Conversation conversation, Long viewerId) {
        var inboxUpdate = dtoBuilder.toResponseDto(conversation, viewerId);
        messagingTemplate.convertAndSendToUser(
                viewerId.toString(),
                "/queue/inbox",
                inboxUpdate
        );
    }
}
