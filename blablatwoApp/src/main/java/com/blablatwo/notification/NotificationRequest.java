package com.blablatwo.notification;

import lombok.Builder;

import java.util.Map;

@Builder
public record NotificationRequest(
        Long recipientId,
        NotificationType type,
        EntityType entityType,
        String entityId,
        Map<String, String> params,
        String collapseKey
) {
    public NotificationChannel channel() {
        return type.channel();
    }
}
