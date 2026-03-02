package com.vamigo.messaging;

public enum MessageStatus {
    SENT,
    DELIVERED,
    READ;

    /**
     * Derive status from the entity's delivery/read timestamps.
     */
    public static MessageStatus fromTimestamps(java.time.Instant deliveredAt, java.time.Instant readAt) {
        if (readAt != null) return READ;
        if (deliveredAt != null) return DELIVERED;
        return SENT;
    }
}
