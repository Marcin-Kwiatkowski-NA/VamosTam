package com.vamigo.notification;

import com.vamigo.user.UserAccount;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "notification", indexes = {
        @Index(name = "idx_notif_recipient_created", columnList = "recipient_id, created_at DESC"),
        @Index(name = "idx_notif_recipient_unread", columnList = "recipient_id, read_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserAccount recipient;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 30)
    private EntityType entityType;

    @Column(name = "entity_id", nullable = false, length = 100)
    private String entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> params;

    @Column(name = "collapse_key", length = 200)
    private String collapseKey;

    @Builder.Default
    @Column(nullable = false)
    private int count = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public boolean isRead() {
        return readAt != null;
    }

    /**
     * Collapse: bump count and refresh timestamp for repeated events.
     */
    public void collapse(Map<String, String> newParams) {
        this.count++;
        this.createdAt = Instant.now();
        if (newParams != null) {
            this.params = newParams;
        }
    }
}
