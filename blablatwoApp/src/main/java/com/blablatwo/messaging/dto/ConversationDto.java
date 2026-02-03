package com.blablatwo.messaging.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationDto(
    UUID id,
    Long rideId,
    Long driverId,
    String driverName,
    Long passengerId,
    String passengerName,
    String originName,
    String destinationName,
    MessageDto lastMessage,
    int unreadCount,
    Instant updatedAt
) {}
