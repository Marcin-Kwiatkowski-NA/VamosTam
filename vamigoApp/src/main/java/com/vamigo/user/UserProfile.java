package com.vamigo.user;

import com.vamigo.user.dto.UpdateProfileRequest;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
public class UserProfile {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private UserAccount account;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "avatar_object_key", length = 300)
    private String avatarObjectKey;

    @Column(length = 500)
    private String bio;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    @Builder.Default
    private AccountType accountType = AccountType.PRIVATE;

    @Embedded
    @Builder.Default
    private UserStats stats = new UserStats();

    public void updateFrom(UpdateProfileRequest request) {
        if (request.displayName() != null) {
            this.displayName = request.displayName();
        }
        if (request.bio() != null) {
            this.bio = request.bio();
        }
        if (request.phoneNumber() != null) {
            this.phoneNumber = request.phoneNumber();
        }
    }

    public void updateDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void updateAvatar(String objectKey) {
        this.avatarObjectKey = objectKey;
    }

    public void clearAvatar() {
        this.avatarObjectKey = null;
    }
}
