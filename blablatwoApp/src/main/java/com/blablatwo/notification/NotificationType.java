package com.blablatwo.notification;

public enum NotificationType {

    CHAT_MESSAGE_NEW,

    BOOKING_REQUESTED,
    BOOKING_CONFIRMED,
    BOOKING_REJECTED,
    BOOKING_CANCELLED,
    BOOKING_EXPIRED,

    RIDE_COMPLETED,
    REVIEW_RECEIVED,
    REVIEW_REMINDER;

    public NotificationChannel channel() {
        return switch (this) {
            case CHAT_MESSAGE_NEW -> NotificationChannel.MESSAGES;
            case BOOKING_REQUESTED, BOOKING_CONFIRMED, BOOKING_REJECTED,
                 BOOKING_CANCELLED, BOOKING_EXPIRED -> NotificationChannel.BOOKING_UPDATES;
            case RIDE_COMPLETED, REVIEW_RECEIVED, REVIEW_REMINDER -> NotificationChannel.REVIEWS;
        };
    }

    public String pushTitle() {
        return switch (this) {
            case BOOKING_REQUESTED, BOOKING_CONFIRMED, BOOKING_REJECTED,
                 BOOKING_CANCELLED, BOOKING_EXPIRED -> "Booking update";
            case RIDE_COMPLETED -> "Ride completed";
            case REVIEW_RECEIVED -> "New review";
            case REVIEW_REMINDER -> "Leave a review";
            case CHAT_MESSAGE_NEW -> "New message";
        };
    }

    public String pushBody() {
        return switch (this) {
            case BOOKING_REQUESTED -> "New booking request — tap for details";
            case BOOKING_CONFIRMED -> "Booking confirmed — tap for details";
            case BOOKING_REJECTED -> "Booking rejected — tap for details";
            case BOOKING_CANCELLED -> "Booking cancelled — tap for details";
            case BOOKING_EXPIRED -> "Booking expired — tap for details";
            case RIDE_COMPLETED -> "Your ride has ended — rate your experience";
            case REVIEW_RECEIVED -> "Someone left you a review — tap to see";
            case REVIEW_REMINDER -> "Don't forget to rate your ride";
            case CHAT_MESSAGE_NEW -> "New message — tap to view";
        };
    }
}
