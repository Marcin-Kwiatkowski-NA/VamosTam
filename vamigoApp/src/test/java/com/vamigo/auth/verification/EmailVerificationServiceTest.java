package com.vamigo.auth.verification;

import com.vamigo.auth.exception.InvalidTokenException;
import com.vamigo.auth.exception.VerificationCooldownException;
import com.vamigo.email.BrevoClient;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private EmailVerificationTokenRepository tokenRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private BrevoClient brevoClient;

    private EmailVerificationService service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationService(
                tokenRepository, userAccountRepository, brevoClient,
                24, 60, "https://localhost:8443", 2L, 1L
        );
    }

    @Nested
    class SendVerificationEmail {

        @Test
        void invalidatesOldTokensAndSavesNewHashedToken() {
            UserAccount user = UserAccount.builder().id(1L).email("test@example.com").build();
            when(tokenRepository.findLatestCreatedAtForUser(1L)).thenReturn(Optional.empty());

            service.sendVerificationEmail(user, Locale.ENGLISH);

            verify(tokenRepository).invalidateAllForUser(1L);

            ArgumentCaptor<EmailVerificationToken> captor = ArgumentCaptor.forClass(EmailVerificationToken.class);
            verify(tokenRepository).save(captor.capture());

            EmailVerificationToken saved = captor.getValue();
            assertThat(saved.getTokenHash()).hasSize(64); // SHA-256 hex
            assertThat(saved.getUser()).isEqualTo(user);
            assertThat(saved.getExpiresAt()).isAfter(Instant.now());
        }

        @Test
        void sendsEmailViaBrevoWithTemplateIdAndParams() {
            UserAccount user = UserAccount.builder().id(1L).email("test@example.com").build();
            when(tokenRepository.findLatestCreatedAtForUser(1L)).thenReturn(Optional.empty());

            service.sendVerificationEmail(user, Locale.ENGLISH);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(brevoClient).sendTemplateEmail(
                    eq("test@example.com"),
                    eq("test"),
                    eq(2L),
                    paramsCaptor.capture()
            );

            Map<String, String> params = paramsCaptor.getValue();
            assertThat(params).containsKey("VERIFICATION_URL");
            assertThat(params).containsKey("USER_NAME");
            assertThat(params.get("VERIFICATION_URL")).startsWith("https://localhost:8443/auth/verify-email?token=");
        }

        @Test
        void usesPolishTemplateWhenLocaleIsPolish() {
            UserAccount user = UserAccount.builder().id(1L).email("test@example.com").build();
            when(tokenRepository.findLatestCreatedAtForUser(1L)).thenReturn(Optional.empty());

            service.sendVerificationEmail(user, Locale.forLanguageTag("pl"));

            verify(brevoClient).sendTemplateEmail(
                    eq("test@example.com"),
                    eq("test"),
                    eq(1L),
                    any()
            );
        }

        @Test
        void throwsCooldownExceptionWhenTokenCreatedRecently() {
            UserAccount user = UserAccount.builder().id(1L).email("test@example.com").build();
            when(tokenRepository.findLatestCreatedAtForUser(1L))
                    .thenReturn(Optional.of(Instant.now().minusSeconds(30)));

            assertThatThrownBy(() -> service.sendVerificationEmail(user, Locale.ENGLISH))
                    .isInstanceOf(VerificationCooldownException.class);

            verify(tokenRepository, never()).save(any());
            verify(brevoClient, never()).sendTemplateEmail(any(), any(), anyLong(), any());
        }

        @Test
        void allowsSendWhenCooldownHasElapsed() {
            UserAccount user = UserAccount.builder().id(1L).email("test@example.com").build();
            when(tokenRepository.findLatestCreatedAtForUser(1L))
                    .thenReturn(Optional.of(Instant.now().minusSeconds(61)));

            service.sendVerificationEmail(user, Locale.ENGLISH);

            verify(tokenRepository).save(any());
            verify(brevoClient).sendTemplateEmail(any(), any(), anyLong(), any());
        }
    }

    @Nested
    class VerifyEmail {

        @Test
        void setsEmailVerifiedAtOnValidToken() {
            String plainToken = "valid-token";
            String tokenHash = TokenHashUtil.hashToken(plainToken);
            UserAccount user = UserAccount.builder().id(1L).email("test@example.com").build();
            EmailVerificationToken token = EmailVerificationToken.builder()
                    .tokenHash(tokenHash)
                    .user(user)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

            service.verifyEmail(plainToken);

            assertThat(token.getUsedAt()).isNotNull();
            assertThat(user.getEmailVerifiedAt()).isNotNull();
            verify(userAccountRepository).save(user);
        }

        @Test
        void throwsOnNonexistentToken() {
            String plainToken = "nonexistent";
            String tokenHash = TokenHashUtil.hashToken(plainToken);
            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.verifyEmail(plainToken))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid");
        }

        @Test
        void throwsOnAlreadyUsedToken() {
            String plainToken = "used-token";
            String tokenHash = TokenHashUtil.hashToken(plainToken);
            EmailVerificationToken token = EmailVerificationToken.builder()
                    .tokenHash(tokenHash)
                    .user(UserAccount.builder().id(1L).build())
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .usedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.verifyEmail(plainToken))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("already been used");
        }

        @Test
        void throwsOnExpiredToken() {
            String plainToken = "expired-token";
            String tokenHash = TokenHashUtil.hashToken(plainToken);
            EmailVerificationToken token = EmailVerificationToken.builder()
                    .tokenHash(tokenHash)
                    .user(UserAccount.builder().id(1L).build())
                    .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.verifyEmail(plainToken))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expired");
        }
    }
}
