package com.vamigo.notification.dto;

import com.vamigo.notification.EntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Body for {@code POST /me/notifications/read-by-entity}.
 * Drives context-aware mark-read: opening a conversation/ride/seat/review
 * detail screen clears all of the caller's unread bell rows targeting it.
 */
public record MarkReadByEntityRequest(
        @NotNull EntityType entityType,
        @NotBlank String entityId
) {
}
