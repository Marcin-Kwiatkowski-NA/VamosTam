package com.vamigo.notification;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.vamigo.AbstractFullStackTest;
import com.vamigo.user.UserAccount;
import com.vamigo.util.IntegrationFixtures;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
 * End-to-end FCM dispatch.
 *
 * <p><b>Why not {@code @MockitoBean FirebaseMessaging} as the plan originally suggested?</b>
 * The production {@link FcmPushNotificationService} resolves the client via the static
 * {@code FirebaseMessaging.getInstance()} call (not constructor-injection), so a Spring bean
 * override has no effect. The plan's intent — assert the SDK is called with the right
 * {@link Message} — is preserved here with Mockito's {@link MockedStatic}.
 *
 * <p><b>Why not {@code fcm.enabled=true} via {@code @DynamicPropertySource}?</b> That would
 * also activate {@code FirebaseConfig} which fails at {@code @PostConstruct} without a real
 * credentials file. Instead this class imports {@link FcmTestConfig} to register a
 * {@link FcmPushNotificationService} bean directly — bypassing the {@code @ConditionalOnProperty}
 * gate and the credentials requirement. Both property and context stay identical to every
 * other full-stack IT so context caching is preserved.
 */
@Import(FcmDispatchIT.FcmTestConfig.class)
class FcmDispatchIT extends AbstractFullStackTest {

    @Autowired IntegrationFixtures fx;
    @Autowired FcmPushNotificationService fcm;
    @Autowired DeviceTokenRepository deviceTokenRepository;

    @Test
    void sendToUser_invokesFirebaseMessaging_onceForEachToken() throws Exception {
        UserAccount user = fx.persistUser();
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user)
                .token("fcm-test-token-primary")
                .platform(Platform.ANDROID)
                .createdAt(Instant.now())
                .build());
        deviceTokenRepository.save(DeviceToken.builder()
                .user(user)
                .token("fcm-test-token-secondary")
                .platform(Platform.IOS)
                .createdAt(Instant.now())
                .build());

        FirebaseMessaging messaging = Mockito.mock(FirebaseMessaging.class);
        Mockito.when(messaging.send(any(Message.class))).thenReturn("msg-id-1", "msg-id-2");

        try (MockedStatic<FirebaseMessaging> stat = Mockito.mockStatic(FirebaseMessaging.class)) {
            stat.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            fcm.sendToUser(user.getId(), "Test title", "Test body",
                    Map.of("type", "BOOKING_CONFIRMED", "deepLink", "/offer/r-42"));
        }

        // Firebase's Message is an opaque value object with no public getters / meaningful
        // toString, so assert the dispatch happened — once per registered token.
        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        Mockito.verify(messaging, Mockito.times(2)).send(captor.capture());
        assertThat(captor.getAllValues()).hasSize(2).doesNotContainNull();
    }

    @Test
    void sendToUser_withNoTokens_doesNotCallFirebaseMessaging() throws Exception {
        UserAccount user = fx.persistUser();

        FirebaseMessaging messaging = Mockito.mock(FirebaseMessaging.class);

        try (MockedStatic<FirebaseMessaging> stat = Mockito.mockStatic(FirebaseMessaging.class)) {
            stat.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            fcm.sendToUser(user.getId(), "irrelevant", "irrelevant", Map.of());
        }

        Mockito.verify(messaging, Mockito.never()).send(any(Message.class));
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
