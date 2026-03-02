package com.vamigo.messaging.event;

import com.vamigo.messaging.Conversation;
import com.vamigo.messaging.ConversationDtoBuilder;
import com.vamigo.messaging.ConversationRepository;
import com.vamigo.messaging.MessageRepository;
import com.vamigo.messaging.MessageStatus;
import com.vamigo.messaging.dto.MessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Broadcasts message events over STOMP.
 *
 * <p>Runs {@code @Async} in a separate thread with its own read-only
 * transaction ({@code REQUIRES_NEW}), so all JPA lazy associations and
 * ride/seat lookups in {@link ConversationDtoBuilder} are resolved within
 * a live Hibernate session.
 */
@Service
public class MessageBroadcastService {

    private static final Logger log = LoggerFactory.getLogger(MessageBroadcastService.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationDtoBuilder dtoBuilder;

    public MessageBroadcastService(SimpMessagingTemplate messagingTemplate,
                                    MessageRepository messageRepository,
                                    ConversationRepository conversationRepository,
                                    ConversationDtoBuilder dtoBuilder) {
        this.messagingTemplate = messagingTemplate;
        this.messageRepository = messageRepository;
        this.conversationRepository = conversationRepository;
        this.dtoBuilder = dtoBuilder;
    }

    @Async
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void broadcast(MessageCreatedEvent event) {
        var message = messageRepository.findById(event.messageId()).orElse(null);
        if (message == null) {
            log.warn("Message {} not found for broadcast (conversation={})",
                    event.messageId(), event.conversationId());
            return;
        }

        // 1. Broadcast message to conversation topic
        var messagePayload = new MessageDto(
                message.getId(),
                event.conversationId(),
                event.senderId(),
                false, // isMine is viewer-relative; clients resolve via senderId
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
        var conversation = conversationRepository
                .findByIdWithParticipants(event.conversationId())
                .orElse(null);
        if (conversation == null) {
            log.warn("Conversation {} not found for inbox broadcast (message={})",
                    event.conversationId(), event.messageId());
            return;
        }

        sendInboxUpdate(conversation, conversation.getParticipantA().getId(), event);
        sendInboxUpdate(conversation, conversation.getParticipantB().getId(), event);
    }

    private void sendInboxUpdate(Conversation conversation, Long viewerId, MessageCreatedEvent event) {
        try {
            var inboxUpdate = dtoBuilder.toResponseDto(conversation, viewerId);
            messagingTemplate.convertAndSendToUser(
                    viewerId.toString(),
                    "/queue/inbox",
                    inboxUpdate
            );
        } catch (Exception e) {
            log.error("Failed to send inbox update: conversation={} viewer={} message={}",
                    conversation.getId(), viewerId, event.messageId(), e);
        }
    }
}