package com.vamigo.messaging;

import com.vamigo.messaging.dto.MessageDto;
import com.vamigo.messaging.dto.SendMessageRequest;
import com.vamigo.messaging.event.MessageCreatedEvent;
import com.vamigo.messaging.exception.ConversationNotFoundException;
import com.vamigo.user.UserAccountRepository;
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
 * {@link com.vamigo.messaging.event.MessageBroadcastListener}
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

        conversation.recordNewMessage(savedMessage, senderId);
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
