package com.vamigo.user;

import com.vamigo.auth.AuthProvider;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_account")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_account_providers", joinColumns = @JoinColumn(name = "user_account_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "provider")
    @Builder.Default
    private Set<AuthProvider> providers = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_account_roles", joinColumns = @JoinColumn(name = "user_account_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(name = "google_id", unique = true)
    private String googleId;

    @Column(name = "failed_login_attempts")
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "phone_verified_at")
    private Instant phoneVerifiedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private int version;

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    public void addRole(Role role) {
        roles.add(role);
    }

    public void addProvider(AuthProvider provider) {
        providers.add(provider);
    }

    public boolean hasProvider(AuthProvider provider) {
        return providers.contains(provider);
    }

    public boolean isTemporarilyLocked() {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil);
    }

    public void recordFailedLogin(int maxAttempts, int lockDurationMinutes) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = Instant.now().plus(Duration.ofMinutes(lockDurationMinutes));
        }
    }

    public void resetFailedLoginAttempts() {
        failedLoginAttempts = 0;
        lockedUntil = null;
    }
}
