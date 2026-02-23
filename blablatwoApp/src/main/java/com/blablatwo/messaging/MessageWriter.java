package com.blablatwo.messaging;

import com.blablatwo.messaging.dto.MessageDto;
import com.blablatwo.messaging.dto.SendMessageRequest;
import com.blablatwo.messaging.event.MessageCreatedEvent;
import com.blablatwo.messaging.exception.ConversationNotFoundException;
import com.blablatwo.user.UserAccountRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Atomic message write in its own transaction.
 *
 * Extracted from {@link ConversationServiceImpl} so that
 * {@code @Transactional(REQUIRES_NEW)} goes through Spring's proxy
 * (no self-invocation bypass) and the
 * {@code @TransactionalEventListener(AFTER_COMMIT)} in
 * {@link com.blablatwo.messaging.event.MessageBroadcastListener}
 * fires reliably after commit.
 */
@Component
public class MessageWriter {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserAccountRepository userAccountRepository;
    private final ApplicationEventPublisher eventPublisher;

    public MessageWriter(ConversationRepository conversationRepository,
                         MessageRepository messageRepository,
                         UserAccountRepository userAccountRepository,
                         ApplicationEventPublisher eventPublisher) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userAccountRepository = userAccountRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MessageDto writeMessage(UUID conversationId, SendMessageRequest request, Long senderId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        Message message = Message.builder()
                .conversation(conversation)
                .sender(userAccountRepository.getReferenceById(senderId))
                .body(request.body())
                .build();

        Message savedMessage = messageRepository.save(message);

        // Update conversation denormalized fields
        conversation.setLastMessageId(savedMessage.getId());
        conversation.setLastMessageBody(savedMessage.getBody());
        conversation.setLastMessageCreatedAt(savedMessage.getCreatedAt());
        conversation.setLastMessageSenderId(senderId);

        // Update unread counts
        boolean isParticipantA = conversation.getParticipantA().getId().equals(senderId);
        if (isParticipantA) {
            conversation.setParticipantBUnreadCount(conversation.getParticipantBUnreadCount() + 1);
            conversation.setParticipantAUnreadCount(0);
        } else {
            conversation.setParticipantAUnreadCount(conversation.getParticipantAUnreadCount() + 1);
            conversation.setParticipantBUnreadCount(0);
        }

        conversationRepository.save(conversation);

        // Publish event (listener fires after commit)
        eventPublisher.publishEvent(new MessageCreatedEvent(
                savedMessage.getId(),
                conversationId,
                senderId,
                savedMessage.getCreatedAt()
        ));

        return new MessageDto(
                savedMessage.getId(),
                conversationId,
                senderId,
                true,
                savedMessage.getBody(),
                savedMessage.getCreatedAt(),
                MessageStatus.SENT,
                savedMessage.getMessageType()
        );
    }
}
