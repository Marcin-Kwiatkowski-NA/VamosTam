package com.vamigo.notification;

import lombok.Builder;

import java.util.Map;

@Builder
public record NotificationRequest(
        Long recipientId,
        NotificationType type,
        EntityType entityType,
        String entityId,
        TargetType targetType,
        ResultKind resultKind,
        Map<String, Object> listFilters,
        Map<String, String> params,
        String collapseKey
) {
    public NotificationChannel channel() {
        return type.channel();
    }

    public TargetType effectiveTargetType() {
        return targetType != null ? targetType : TargetType.ENTITY;
    }
}
