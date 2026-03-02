package com.blablatwo.user;

import com.blablatwo.config.StorageProperties;
import org.springframework.stereotype.Component;

@Component
public class AvatarUrlResolver {

    private final StorageProperties storageProperties;

    public AvatarUrlResolver(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    public String resolve(UserProfile profile) {
        if (profile == null || profile.getAvatarObjectKey() == null) {
            return null;
        }
        return storageProperties.publicUrlBase() + "/" + profile.getAvatarObjectKey();
    }
}
