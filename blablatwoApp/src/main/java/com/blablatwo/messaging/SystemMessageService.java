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

    /**
     * Posts a system message to a specific conversation between two participants.
     *
     * @param topicKey       the offer topic key (e.g. "offer:r-123")
     * @param participantAId one participant
     * @param participantBId the other participant
     * @param bodyKey        localization key
     * @param silent         if true, the message won't bump the conversation in the inbox
     */
    @Transactional
    public void postSystemMessageToConversation(String topicKey, Long participantAId,
                                                  Long participantBId, String bodyKey, boolean silent) {
        // Try both orderings since participant order may vary
        conversationRepository.findByTopicKeyAndParticipants(topicKey, participantAId, participantBId)
                .or(() -> conversationRepository.findByTopicKeyAndParticipants(topicKey, participantBId, participantAId))
                .ifPresent(conversation -> {
                    if (silent) {
                        postToConversationSilent(conversation, participantAId, bodyKey);
                    } else {
                        postToConversation(conversation, participantAId, bodyKey);
                    }
                });
    }

    private void postToConversationSilent(Conversation conversation, Long actorId, String bodyKey) {
        Message message = Message.builder()
                .conversation(conversation)
                .sender(userAccountRepository.getReferenceById(actorId))
                .body(bodyKey)
                .messageType(MessageType.SYSTEM)
                .build();

        messageRepository.save(message);
        // Silent: do NOT update conversation denormalized fields or publish event
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
