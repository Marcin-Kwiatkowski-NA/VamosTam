package com.vamigo.notification;

/**
 * Describes how the client should navigate when a notification is tapped.
 *
 * <ul>
 *   <li>{@link #ENTITY} — open a single entity detail screen; pair with
 *       {@code entityType} + {@code entityId}.</li>
 *   <li>{@link #LIST} — open a filtered list; pair with {@code resultKind}
 *       + {@code listFilters}.</li>
 *   <li>{@link #NONE} — no navigation target; client opens the notification
 *       center.</li>
 * </ul>
 */
public enum TargetType {
    ENTITY,
    LIST,
    NONE
}
