package com.vamigo.notification.dto;

import com.vamigo.notification.EntityType;
import com.vamigo.notification.Notification;
import com.vamigo.notification.NotificationChannel;
import com.vamigo.notification.NotificationType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponseDto(
        UUID id,
        NotificationType type,
        NotificationChannel channel,
        EntityType entityType,
        String entityId,
        Map<String, String> params,
        String collapseKey,
        int count,
        Instant createdAt,
        Instant readAt
) {
    public static NotificationResponseDto from(Notification n) {
        return new NotificationResponseDto(
                n.getId(),
                n.getNotificationType(),
                n.getChannel(),
                n.getEntityType(),
                n.getEntityId(),
                n.getParams(),
                n.getCollapseKey(),
                n.getCount(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}
