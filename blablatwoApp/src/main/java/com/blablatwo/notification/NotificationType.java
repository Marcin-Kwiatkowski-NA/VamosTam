package com.blablatwo.notification;

public enum NotificationType {

    CHAT_MESSAGE_NEW,

    BOOKING_REQUESTED,
    BOOKING_CONFIRMED,
    BOOKING_REJECTED,
    BOOKING_CANCELLED,
    BOOKING_EXPIRED;

    public NotificationChannel channel() {
        return switch (this) {
            case CHAT_MESSAGE_NEW -> NotificationChannel.MESSAGES;
            case BOOKING_REQUESTED, BOOKING_CONFIRMED, BOOKING_REJECTED,
                 BOOKING_CANCELLED, BOOKING_EXPIRED -> NotificationChannel.BOOKING_UPDATES;
        };
    }
}
