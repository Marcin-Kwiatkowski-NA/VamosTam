package com.blablatwo.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "fcm.enabled", havingValue = "true")
public class FcmPushNotificationService implements PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(FcmPushNotificationService.class);

    private final DeviceTokenRepository deviceTokenRepository;

    public FcmPushNotificationService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendToUser(Long userId, String title, String body, Map<String, String> data) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            log.debug("No device tokens for user {}, skipping push", userId);
            return;
        }

        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(body)
                .build();

        for (DeviceToken deviceToken : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(deviceToken.getToken())
                        .setNotification(notification)
                        .putAllData(data)
                        .build();

                FirebaseMessaging.getInstance().send(message);
                log.debug("Push sent to user {} on device {}", userId, deviceToken.getId());

            } catch (FirebaseMessagingException e) {
                if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                        || e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
                    log.info("Removing stale device token {} for user {}", deviceToken.getId(), userId);
                    deviceTokenRepository.delete(deviceToken);
                } else {
                    log.error("Failed to send push to user {} device {}: {}",
                            userId, deviceToken.getId(), e.getMessage());
                }
            }
        }
    }
}
