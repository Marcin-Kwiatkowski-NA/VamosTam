package com.blablatwo.messaging;

import com.blablatwo.messaging.event.MessageCreatedEvent;
import com.blablatwo.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creates system messages (e.g. "Booking confirmed") in conversation threads
 * linked to a given topicKey.
 */
@Service
public class SystemMessageService {

    private static final Logger log = LoggerFactory.getLogger(SystemMessageService.class);

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserAccountRepository userAccountRepository;
    private final ApplicationEventPublisher eventPublisher;

    public SystemMessageService(ConversationRepository conversationRepository,
                                 MessageRepository messageRepository,
                                 UserAccountRepository userAccountRepository,
                                 ApplicationEventPublisher eventPublisher) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userAccountRepository = userAccountRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Posts a system message into all conversations with the given topicKey.
     *
     * @param topicKey the offer topic key (e.g. "offer:r-123")
     * @param actorId  the user who triggered the event (used as sender for attribution)
     * @param bodyKey  a stable key localized on the frontend (e.g. "system.booking_confirmed")
     */
    @Transactional
    public void postSystemMessage(String topicKey, Long actorId, String bodyKey) {
        List<Conversation> conversations = conversationRepository.findByTopicKey(topicKey);

        if (conversations.isEmpty()) {
            log.debug("No conversations found for topicKey={}, skipping system message", topicKey);
            return;
        }

        for (Conversation conversation : conversations) {
            postToConversation(conversation, actorId, bodyKey);
        }
    }

    private void postToConversation(Conversation conversation, Long actorId, String bodyKey) {
        Message message = Message.builder()
                .conversation(conversation)
                .sender(userAccountRepository.getReferenceById(actorId))
                .body(bodyKey)
                .messageType(MessageType.SYSTEM)
                .build();

        Message saved = messageRepository.save(message);

        // Update conversation denormalized fields
        conversation.setLastMessageId(saved.getId());
        conversation.setLastMessageBody(saved.getBody());
        conversation.setLastMessageCreatedAt(saved.getCreatedAt());
        conversation.setLastMessageSenderId(actorId);

        conversationRepository.save(conversation);

        eventPublisher.publishEvent(new MessageCreatedEvent(
                saved.getId(),
                conversation.getId(),
                actorId,
                saved.getCreatedAt()
        ));
    }
}
