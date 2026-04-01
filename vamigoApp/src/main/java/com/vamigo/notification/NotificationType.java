package com.vamigo.notification;

public enum NotificationType {

    CHAT_MESSAGE_NEW,

    BOOKING_REQUESTED,
    BOOKING_CONFIRMED,
    BOOKING_REJECTED,
    BOOKING_CANCELLED,
    BOOKING_EXPIRED,

    RIDE_COMPLETED,
    REVIEW_RECEIVED,
    REVIEW_REMINDER,

    SEARCH_ALERT_MATCH;

    public NotificationChannel channel() {
        return switch (this) {
            case CHAT_MESSAGE_NEW -> NotificationChannel.MESSAGES;
            case BOOKING_REQUESTED, BOOKING_CONFIRMED, BOOKING_REJECTED,
                 BOOKING_CANCELLED, BOOKING_EXPIRED -> NotificationChannel.BOOKING_UPDATES;
            case RIDE_COMPLETED, REVIEW_RECEIVED, REVIEW_REMINDER -> NotificationChannel.REVIEWS;
            case SEARCH_ALERT_MATCH -> NotificationChannel.SEARCH_ALERTS;
        };
    }

}
