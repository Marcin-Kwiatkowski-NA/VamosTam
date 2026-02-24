package com.blablatwo.notification.dto;

import com.blablatwo.notification.EntityType;
import com.blablatwo.notification.Notification;
import com.blablatwo.notification.NotificationChannel;
import com.blablatwo.notification.NotificationType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Lightweight STOMP payload sent to {@code /user/queue/notifications}.
 * Includes {@code unreadCount} so the client can update the badge
 * without an extra REST call.
 */
public record NotificationAlertDto(
        UUID id,
        NotificationType type,
        NotificationChannel channel,
        EntityType entityType,
        String entityId,
        Map<String, String> params,
        String collapseKey,
        int count,
        Instant createdAt,
        long unreadCount
) {
    public static NotificationAlertDto from(Notification n, long unreadCount) {
        return new NotificationAlertDto(
                n.getId(),
                n.getNotificationType(),
                n.getChannel(),
                n.getEntityType(),
                n.getEntityId(),
                n.getParams(),
                n.getCollapseKey(),
                n.getCount(),
                n.getCreatedAt(),
                unreadCount
        );
    }
}
