package com.blablatwo.messaging;

import com.blablatwo.messaging.dto.ConversationDto;
import com.blablatwo.messaging.dto.CreateConversationRequest;
import com.blablatwo.messaging.dto.MessageDto;
import com.blablatwo.messaging.dto.SendMessageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ConversationService {

    record InitResult(ConversationDto conversation, boolean created) {}

    InitResult initConversation(CreateConversationRequest request, Long passengerId);

    List<ConversationDto> listConversations(Long travelerId, Instant since, Pageable pageable);

    List<MessageDto> getMessages(UUID conversationId, Long travelerId, Instant before, Instant since, int limit);

    MessageDto sendMessage(UUID conversationId, SendMessageRequest request, Long senderId);
}
