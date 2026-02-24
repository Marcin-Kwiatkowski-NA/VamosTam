package com.blablatwo.notification.dto;

import java.util.List;

public record NotificationPageDto(
        List<NotificationResponseDto> notifications,
        boolean hasMore,
        long unreadCount
) {
}
