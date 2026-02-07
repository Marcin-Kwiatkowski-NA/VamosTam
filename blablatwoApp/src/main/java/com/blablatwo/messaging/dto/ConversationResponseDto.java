package com.blablatwo.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponseDto(
    UUID id,
    String topicKey,
    PeerUserDto peerUser,
    String lastMessage,
    Instant lastMessageAt,
    int unreadCount
) {}
