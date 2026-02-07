package com.blablatwo.messaging;

import com.blablatwo.messaging.dto.ConversationOpenRequest;
import com.blablatwo.messaging.dto.ConversationResponseDto;
import com.blablatwo.messaging.dto.MessageDto;
import com.blablatwo.messaging.dto.SendMessageRequest;
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
}
