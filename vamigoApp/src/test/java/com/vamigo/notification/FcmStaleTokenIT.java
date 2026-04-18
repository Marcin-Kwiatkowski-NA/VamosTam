package com.vamigo.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.vamigo.AbstractFullStackTest;
import com.vamigo.user.UserAccount;
import com.vamigo.util.IntegrationFixtures;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

/**
 * When Firebase reports {@code UNREGISTERED} (a stale/invalid token), the matching
 * {@link DeviceToken} row is removed — otherwise the backend would keep pushing to
 * ghost devices and gradually accumulate garbage.
 *
 * <p>See {@link FcmDispatchIT} for why this uses {@link MockedStatic} instead of
 * {@code @MockitoBean FirebaseMessaging} and a test-scoped bean definition instead of
 * flipping {@code fcm.enabled=true}.
 */
@Import(FcmStaleTokenIT.FcmTestConfig.class)
class FcmStaleTokenIT extends AbstractFullStackTest {

    @Autowired IntegrationFixtures fx;
    @Autowired FcmPushNotificationService fcm;
    @Autowired DeviceTokenRepository deviceTokenRepository;

    @Test
    void unregisteredErrorCode_deletesStaleDeviceToken() throws Exception {
        UserAccount user = fx.persistUser();
        DeviceToken stale = deviceTokenRepository.save(DeviceToken.builder()
                .user(user)
                .token("stale-fcm-token")
                .platform(Platform.ANDROID)
                .createdAt(Instant.now())
                .build());

        FirebaseMessaging messaging = Mockito.mock(FirebaseMessaging.class);
        FirebaseMessagingException unregistered = Mockito.mock(FirebaseMessagingException.class);
        Mockito.when(unregistered.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        Mockito.when(messaging.send(any(Message.class))).thenThrow(unregistered);

        try (MockedStatic<FirebaseMessaging> stat = Mockito.mockStatic(FirebaseMessaging.class)) {
            stat.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            fcm.sendToUser(user.getId(), "title", "body", Map.of());
        }

        assertThat(deviceTokenRepository.findById(stale.getId())).isEmpty();
        assertThat(deviceTokenRepository.findByUserId(user.getId())).isEmpty();
    }

    @Test
    void invalidArgumentErrorCode_deletesStaleDeviceToken() throws Exception {
        UserAccount user = fx.persistUser();
        DeviceToken stale = deviceTokenRepository.save(DeviceToken.builder()
                .user(user)
                .token("malformed-token")
                .platform(Platform.IOS)
                .createdAt(Instant.now())
                .build());

        FirebaseMessaging messaging = Mockito.mock(FirebaseMessaging.class);
        FirebaseMessagingException invalid = Mockito.mock(FirebaseMessagingException.class);
        Mockito.when(invalid.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INVALID_ARGUMENT);
        Mockito.when(messaging.send(any(Message.class))).thenThrow(invalid);

        try (MockedStatic<FirebaseMessaging> stat = Mockito.mockStatic(FirebaseMessaging.class)) {
            stat.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            fcm.sendToUser(user.getId(), "t", "b", Map.of());
        }

        assertThat(deviceTokenRepository.findById(stale.getId())).isEmpty();
    }

    @Test
    void transientErrorCode_preservesDeviceToken() throws Exception {
        UserAccount user = fx.persistUser();
        DeviceToken token = deviceTokenRepository.save(DeviceToken.builder()
                .user(user)
                .token("temporarily-unreachable")
                .platform(Platform.ANDROID)
                .createdAt(Instant.now())
                .build());

        FirebaseMessaging messaging = Mockito.mock(FirebaseMessaging.class);
        FirebaseMessagingException transientErr = Mockito.mock(FirebaseMessagingException.class);
        Mockito.when(transientErr.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNAVAILABLE);
        Mockito.when(messaging.send(any(Message.class))).thenThrow(transientErr);

        try (MockedStatic<FirebaseMessaging> stat = Mockito.mockStatic(FirebaseMessaging.class)) {
            stat.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            fcm.sendToUser(user.getId(), "t", "b", Map.of());
        }

        assertThat(deviceTokenRepository.findById(token.getId())).isPresent();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FcmTestConfig {
        @Bean
        @Primary
        FcmPushNotificationService fcmPushNotificationService(DeviceTokenRepository repo) {
            return new FcmPushNotificationService(repo);
        }
    }
}
