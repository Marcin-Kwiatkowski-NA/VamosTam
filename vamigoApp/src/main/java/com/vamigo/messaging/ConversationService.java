package com.vamigo.messaging;

import com.vamigo.messaging.dto.ConversationOpenRequest;
import com.vamigo.messaging.dto.ConversationResponseDto;
import com.vamigo.messaging.dto.MessageDto;
import com.vamigo.messaging.dto.SendMessageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConversationService {

    record OpenResult(ConversationResponseDto conversation, boolean created) {}

    OpenResult openConversation(ConversationOpenRequest request, Long currentUserId);

    List<ConversationResponseDto> listConversations(Long userId, Instant since, Pageable pageable);

    List<MessageDto> getMessages(UUID conversationId, Long userId, Instant before, Instant since, int limit);

    MessageDto sendMessage(UUID conversationId, SendMessageRequest request, Long senderId);

    void markDelivered(UUID conversationId, UUID lastMessageId, Long userId);

    void markRead(UUID conversationId, UUID lastMessageId, Long userId);
}
