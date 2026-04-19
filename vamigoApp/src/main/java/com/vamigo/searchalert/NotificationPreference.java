package com.vamigo.searchalert;

import com.vamigo.user.UserAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preference")
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
public class NotificationPreference {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Builder.Default
    @Column(name = "search_alerts_push_enabled", nullable = false)
    private boolean searchAlertsPushEnabled = true;

    @Builder.Default
    @Column(name = "search_alerts_email_enabled", nullable = false)
    private boolean searchAlertsEmailEnabled = true;

    @Column(name = "unsubscribe_token", nullable = false, unique = true, length = 64)
    private String unsubscribeToken;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        if (this.unsubscribeToken == null) {
            this.unsubscribeToken = UUID.randomUUID().toString();
        }
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void setSearchAlertsPushEnabled(boolean enabled) {
        this.searchAlertsPushEnabled = enabled;
    }

    public void setSearchAlertsEmailEnabled(boolean enabled) {
        this.searchAlertsEmailEnabled = enabled;
    }
}
