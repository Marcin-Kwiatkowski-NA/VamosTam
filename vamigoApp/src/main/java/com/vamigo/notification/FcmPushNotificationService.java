package com.vamigo.notification;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
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
    /** Matches the Android channel created on the client in notification_channel.dart. */
    private static final String DEFAULT_ANDROID_CHANNEL_ID = "vamigo_default";

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

        // Render BigText on Android when the renderer produced an expanded body.
        // Firebase Admin auto-applies BigText style for multi-line bodies; we
        // also pin the channel ID so collapsed display matches the in-app
        // local-notification channel.
        String bigBody = data.get("bigBody");
        AndroidConfig androidConfig = AndroidConfig.builder()
                .setNotification(AndroidNotification.builder()
                        .setTitle(title)
                        .setBody(bigBody != null && !bigBody.isBlank() ? bigBody : body)
                        .setChannelId(DEFAULT_ANDROID_CHANNEL_ID)
                        .build())
                .build();

        for (DeviceToken deviceToken : tokens) {
            try {
                Message message = Message.builder()
                        .setToken(deviceToken.getToken())
                        .setNotification(notification)
                        .setAndroidConfig(androidConfig)
                        .putAllData(data)
                        .build();

                FirebaseMessaging.getInstance().send(message);
                log.info("[telemetry] push_received user={} type={} targetType={} entityType={} entityId={}",
                        userId,
                        data.getOrDefault("type", ""),
                        data.getOrDefault("targetType", ""),
                        data.getOrDefault("entityType", ""),
                        data.getOrDefault("entityId", ""));

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
