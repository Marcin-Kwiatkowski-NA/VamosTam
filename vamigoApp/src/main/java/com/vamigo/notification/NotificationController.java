package com.vamigo.notification;

import com.vamigo.auth.AppPrincipal;
import com.vamigo.notification.dto.MarkReadByEntityRequest;
import com.vamigo.notification.dto.NotificationPageDto;
import com.vamigo.notification.dto.UnreadCountDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/me/notifications")
@PreAuthorize("hasRole('USER')")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<NotificationPageDto> getNotifications(
            @AuthenticationPrincipal AppPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.getNotifications(principal.userId(), page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountDto> getUnreadCount(
            @AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(notificationService.getUnreadCount(principal.userId()));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable UUID id,
            @AuthenticationPrincipal AppPrincipal principal) {
        notificationService.markRead(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal AppPrincipal principal) {
        notificationService.markAllRead(principal.userId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Context-aware mark-read: clears the caller's unread bell rows targeting
     * a single entity. Idempotent — returns 204 even if 0 rows match.
     */
    @PostMapping("/read-by-entity")
    public ResponseEntity<Void> markReadByEntity(
            @Valid @RequestBody MarkReadByEntityRequest request,
            @AuthenticationPrincipal AppPrincipal principal) {
        notificationService.markReadByEntity(principal.userId(), request.entityType(), request.entityId());
        return ResponseEntity.noContent().build();
    }
}
