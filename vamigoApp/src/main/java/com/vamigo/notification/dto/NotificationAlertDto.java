package com.vamigo.notification.dto;

import com.vamigo.notification.EntityType;
import com.vamigo.notification.Notification;
import com.vamigo.notification.NotificationChannel;
import com.vamigo.notification.NotificationRequest;
import com.vamigo.notification.NotificationType;

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

    /**
     * Build a transient alert from a request (no DB entity).
     * Used for popup-only notifications that skip persistence.
     */
    public static NotificationAlertDto fromRequest(NotificationRequest request, long unreadCount) {
        return new NotificationAlertDto(
                UUID.randomUUID(),
                request.type(),
                request.channel(),
                request.entityType(),
                request.entityId(),
                request.params(),
                request.collapseKey(),
                1,
                Instant.now(),
                unreadCount
        );
    }
}
