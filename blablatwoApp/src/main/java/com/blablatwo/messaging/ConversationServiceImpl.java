package com.blablatwo.messaging;

import com.blablatwo.messaging.dto.ConversationOpenRequest;
import com.blablatwo.messaging.dto.ConversationResponseDto;
import com.blablatwo.messaging.dto.MessageDto;
import com.blablatwo.messaging.dto.PeerUserDto;
import com.blablatwo.messaging.dto.SendMessageRequest;
import com.blablatwo.messaging.event.MessageCreatedEvent;
import com.blablatwo.messaging.exception.ConversationNotFoundException;
import com.blablatwo.messaging.exception.NotParticipantException;
import com.blablatwo.messaging.exception.SelfConversationException;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileRepository;
import com.blablatwo.user.exception.NoSuchUserException;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserAccountRepository userAccountRepository;
    private final UserProfileRepository userProfileRepository;
    private final MessageMapper messageMapper;
    private final ApplicationEventPublisher eventPublisher;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   MessageRepository messageRepository,
                                   UserAccountRepository userAccountRepository,
                                   UserProfileRepository userProfileRepository,
                                   MessageMapper messageMapper,
                                   ApplicationEventPublisher eventPublisher) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userAccountRepository = userAccountRepository;
        this.userProfileRepository = userProfileRepository;
        this.messageMapper = messageMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public OpenResult openConversation(ConversationOpenRequest request, Long currentUserId) {
        if (currentUserId.equals(request.peerUserId())) {
            throw new SelfConversationException();
        }

        // Normalize participant IDs (A = min, B = max)
        Long participantAId = Math.min(currentUserId, request.peerUserId());
        Long participantBId = Math.max(currentUserId, request.peerUserId());

        var existing = conversationRepository
                .findByTopicKeyAndParticipants(request.topicKey(), participantAId, participantBId);

        if (existing.isPresent()) {
            return new OpenResult(toResponseDto(existing.get(), currentUserId), false);
        }

        UserAccount participantA = userAccountRepository.findById(participantAId)
                .orElseThrow(() -> new NoSuchUserException(participantAId));
        UserAccount participantB = userAccountRepository.findById(participantBId)
                .orElseThrow(() -> new NoSuchUserException(participantBId));

        try {
            Conversation conversation = Conversation.builder()
                    .topicKey(request.topicKey())
                    .participantA(participantA)
                    .participantB(participantB)
                    .build();

            Conversation saved = conversationRepository.save(conversation);
            return new OpenResult(toResponseDto(saved, currentUserId), true);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another request created the conversation
            var created = conversationRepository
                    .findByTopicKeyAndParticipants(request.topicKey(), participantAId, participantBId)
                    .orElseThrow(() -> e);
            return new OpenResult(toResponseDto(created, currentUserId), false);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponseDto> listConversations(Long userId, Instant since, Pageable pageable) {
        List<Conversation> conversations;

        if (since != null) {
            conversations = conversationRepository.findByParticipantIdAndUpdatedAtAfter(userId, since, pageable);
        } else {
            conversations = conversationRepository.findByParticipantId(userId, pageable);
        }

        return conversations.stream()
                .map(c -> toResponseDto(c, userId))
                .toList();
    }

    @Override
    @Transactional
    public List<MessageDto> getMessages(UUID conversationId, Long userId, Instant before, Instant since, int limit) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        validateParticipant(conversation, userId);

        List<Message> messages;
        Pageable pageable = PageRequest.of(0, limit);

        if (since != null) {
            // Polling for new messages - limit to prevent abuse after long offline
            messages = messageRepository.findByConversationIdAndCreatedAtAfterOrderByCreatedAtAsc(
                    conversationId, since, pageable);
        } else if (before != null) {
            // Cursor-based "load older" - DESC, then reverse
            messages = messageRepository.findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                    conversationId, before, pageable);
            messages = new ArrayList<>(messages);
            Collections.reverse(messages);
        } else {
            // Initial load - DESC, then reverse for display order
            messages = messageRepository.findByConversationIdOrderByCreatedAtDesc(conversationId, pageable);
            messages = new ArrayList<>(messages);
            Collections.reverse(messages);
        }

        // Reset viewer's unread count (opening thread marks all as read)
        resetUnreadCount(conversation, userId);

        return messages.stream()
                .map(m -> toMessageDto(m, userId))
                .toList();
    }

    @Override
    public MessageDto sendMessage(UUID conversationId, SendMessageRequest request, Long senderId) {
        // Validation (read-only) happens BEFORE transaction to avoid wasting retries
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        validateParticipant(conversation, senderId);

        // Execute the write operation with manual retry
        return executeWithRetry(() ->
                performSendMessage(conversationId, request, senderId)
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected MessageDto performSendMessage(UUID conversationId, SendMessageRequest request, Long senderId) {
        // Re-fetch inside transaction to get latest version
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
                savedMessage.getCreatedAt()
        );
    }

    private <T> T executeWithRetry(Supplier<T> action) {
        int maxAttempts = 3;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            try {
                return action.get();
            } catch (OptimisticLockException | StaleObjectStateException e) {
                if (attempt == maxAttempts - 1) {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("Retry loop exited without result");
    }

    private void validateParticipant(Conversation conversation, Long userId) {
        boolean isParticipantA = conversation.getParticipantA().getId().equals(userId);
        boolean isParticipantB = conversation.getParticipantB().getId().equals(userId);
        if (!isParticipantA && !isParticipantB) {
            throw new NotParticipantException(conversation.getId(), userId);
        }
    }

    private void resetUnreadCount(Conversation conversation, Long userId) {
        boolean isParticipantA = conversation.getParticipantA().getId().equals(userId);
        if (isParticipantA) {
            conversation.setParticipantAUnreadCount(0);
        } else {
            conversation.setParticipantBUnreadCount(0);
        }
        // JPA flushes dirty entities on commit - no explicit save needed
    }

    private ConversationResponseDto toResponseDto(Conversation conversation, Long viewerId) {
        boolean isParticipantA = conversation.getParticipantA().getId().equals(viewerId);

        UserAccount peer = isParticipantA
                ? conversation.getParticipantB()
                : conversation.getParticipantA();

        PeerUserDto peerUser = buildPeerUserDto(peer);

        int unreadCount = isParticipantA
                ? conversation.getParticipantAUnreadCount()
                : conversation.getParticipantBUnreadCount();

        return new ConversationResponseDto(
                conversation.getId(),
                conversation.getTopicKey(),
                peerUser,
                conversation.getLastMessageBody(),
                conversation.getLastMessageCreatedAt(),
                unreadCount
        );
    }

    private PeerUserDto buildPeerUserDto(UserAccount user) {
        UserProfile profile = userProfileRepository.findById(user.getId()).orElse(null);

        String displayName;
        String avatarUrl = null;

        if (profile != null) {
            displayName = (profile.getDisplayName() != null && !profile.getDisplayName().isBlank())
                    ? profile.getDisplayName()
                    : user.getEmail().split("@")[0];
            avatarUrl = profile.getAvatarUrl();
        } else {
            displayName = user.getEmail().split("@")[0];
        }

        return new PeerUserDto(user.getId(), displayName, avatarUrl);
    }

    private MessageDto toMessageDto(Message message, Long viewerId) {
        MessageDto baseDto = messageMapper.toDto(message);
        return new MessageDto(
                baseDto.id(),
                baseDto.conversationId(),
                baseDto.senderId(),
                message.getSender().getId().equals(viewerId),
                baseDto.body(),
                baseDto.createdAt()
        );
    }
}
