package com.blablatwo.messaging;

import com.blablatwo.messaging.dto.ConversationOpenRequest;
import com.blablatwo.messaging.dto.ConversationResponseDto;
import com.blablatwo.messaging.dto.MessageDto;
import com.blablatwo.messaging.dto.SendMessageRequest;
import com.blablatwo.messaging.exception.ConversationNotFoundException;
import com.blablatwo.messaging.exception.NotParticipantException;
import com.blablatwo.messaging.exception.SelfConversationException;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import com.blablatwo.user.exception.NoSuchUserException;
import jakarta.persistence.OptimisticLockException;
import org.hibernate.StaleObjectStateException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
    private final ConversationDtoBuilder dtoBuilder;
    private final MessageMapper messageMapper;
    private final MessageWriter messageWriter;

    public ConversationServiceImpl(ConversationRepository conversationRepository,
                                   MessageRepository messageRepository,
                                   UserAccountRepository userAccountRepository,
                                   ConversationDtoBuilder dtoBuilder,
                                   MessageMapper messageMapper,
                                   MessageWriter messageWriter) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.userAccountRepository = userAccountRepository;
        this.dtoBuilder = dtoBuilder;
        this.messageMapper = messageMapper;
        this.messageWriter = messageWriter;
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
            return new OpenResult(dtoBuilder.toResponseDto(existing.get(), currentUserId), false);
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
            return new OpenResult(dtoBuilder.toResponseDto(saved, currentUserId), true);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another request created the conversation
            var created = conversationRepository
                    .findByTopicKeyAndParticipants(request.topicKey(), participantAId, participantBId)
                    .orElseThrow(() -> e);
            return new OpenResult(dtoBuilder.toResponseDto(created, currentUserId), false);
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
                .map(c -> dtoBuilder.toResponseDto(c, userId))
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

        // Delegate to a separate bean — calls go through the Spring proxy,
        // so @Transactional(REQUIRES_NEW) works correctly on each retry.
        return executeWithRetry(() ->
                messageWriter.writeMessage(conversationId, request, senderId)
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
