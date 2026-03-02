package com.vamigo.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpPushNotificationService implements PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(NoOpPushNotificationService.class);

    @Override
    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
        log.debug("Push notifications disabled, skipping push to user {}: {}", userId, title);
    }
}
