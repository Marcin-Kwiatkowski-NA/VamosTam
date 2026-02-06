package com.blablatwo.domain;

import com.blablatwo.user.UserProfile;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PersonDisplayNameResolver {

    public String resolveInternal(UserProfile profile, Long userId) {
        if (profile == null) {
            throw new IllegalStateException("UserProfile missing for user: " + userId);
        }
        if (profile.getDisplayName() == null || profile.getDisplayName().isBlank()) {
            throw new IllegalStateException("displayName blank for user: " + userId);
        }
        return profile.getDisplayName();
    }

    public String resolveExternal(String authorName, Long entityId) {
        return Objects.requireNonNull(authorName,
                "Author name required for external entity: " + entityId);
    }
}
