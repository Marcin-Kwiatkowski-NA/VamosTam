package com.vamigo.auth.verification;

import com.vamigo.auth.AuthProvider;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private PasswordResetTokenRepository tokenRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private BrevoClient brevoClient;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                tokenRepository, userAccountRepository, passwordEncoder, brevoClient,
                1, 60, "http://localhost:8080/reset", 10L, 11L
        );
    }

    private UserAccount emailUser() {
        return UserAccount.builder()
                .id(1L)
                .email("test@example.com")
                .passwordHash("existing-hash")
                .providers(new HashSet<>(Set.of(AuthProvider.EMAIL)))
                .build();
    }

    private UserAccount googleOnlyUser() {
        return UserAccount.builder()
                .id(2L)
                .email("google@example.com")
                .passwordHash(null)
                .providers(new HashSet<>(Set.of(AuthProvider.GOOGLE)))
                .build();
    }

    @Nested
    class SendResetEmail {

        @Test
        void sendsEmailForEmailOnlyAccount() {
            UserAccount user = emailUser();
            when(userAccountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(tokenRepository.findLatestCreatedAtForUser(1L)).thenReturn(Optional.empty());

            service.sendResetEmail("test@example.com", Locale.ENGLISH);

            verify(tokenRepository).invalidateAllForUser(1L);
            verify(tokenRepository).save(any(PasswordResetToken.class));
            verify(brevoClient).sendTemplateEmail(eq("test@example.com"), eq("test"), eq(10L), any());
        }

        @Test
        void sendsEmailForGoogleOnlyAccount() {
            UserAccount user = googleOnlyUser();
            when(userAccountRepository.findByEmail("google@example.com")).thenReturn(Optional.of(user));
            when(tokenRepository.findLatestCreatedAtForUser(2L)).thenReturn(Optional.empty());

            service.sendResetEmail("google@example.com", Locale.ENGLISH);

            verify(tokenRepository).invalidateAllForUser(2L);

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            verify(tokenRepository).save(captor.capture());
            assertThat(captor.getValue().getTokenHash()).hasSize(64);
            assertThat(captor.getValue().getUser()).isEqualTo(user);

            verify(brevoClient).sendTemplateEmail(eq("google@example.com"), eq("google"), eq(10L), any());
        }

        @Test
        void usesPolishTemplateWhenLocaleIsPolish() {
            UserAccount user = emailUser();
            when(userAccountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(tokenRepository.findLatestCreatedAtForUser(1L)).thenReturn(Optional.empty());

            service.sendResetEmail("test@example.com", Locale.forLanguageTag("pl"));

            verify(brevoClient).sendTemplateEmail(eq("test@example.com"), eq("test"), eq(11L), any());
        }

        @Test
        void buildsResetUrlWithPlainToken() {
            UserAccount user = emailUser();
            when(userAccountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(tokenRepository.findLatestCreatedAtForUser(1L)).thenReturn(Optional.empty());

            service.sendResetEmail("test@example.com", Locale.ENGLISH);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, String>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(brevoClient).sendTemplateEmail(any(), any(), anyLong(), paramsCaptor.capture());

            Map<String, String> params = paramsCaptor.getValue();
            assertThat(params).containsKey("RESET_URL");
            assertThat(params).containsKey("USER_NAME");
            assertThat(params.get("RESET_URL")).startsWith("http://localhost:8080/reset?token=");
        }

        @Test
        void noOpsForNonexistentEmail() {
            when(userAccountRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

            service.sendResetEmail("missing@example.com", Locale.ENGLISH);

            verify(tokenRepository, never()).invalidateAllForUser(anyLong());
            verify(tokenRepository, never()).save(any());
            verify(brevoClient, never()).sendTemplateEmail(any(), any(), anyLong(), any());
        }

        @Test
        void lowercasesEmailBeforeLookup() {
            UserAccount user = emailUser();
            when(userAccountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(tokenRepository.findLatestCreatedAtForUser(1L)).thenReturn(Optional.empty());

            service.sendResetEmail("TEST@EXAMPLE.COM", Locale.ENGLISH);

            verify(userAccountRepository).findByEmail("test@example.com");
        }

        @Test
        void enforcesCooldownForGoogleOnlyAccount() {
            UserAccount user = googleOnlyUser();
            when(userAccountRepository.findByEmail("google@example.com")).thenReturn(Optional.of(user));
            when(tokenRepository.findLatestCreatedAtForUser(2L))
                    .thenReturn(Optional.of(Instant.now().minusSeconds(30)));

            assertThatThrownBy(() -> service.sendResetEmail("google@example.com", Locale.ENGLISH))
                    .isInstanceOf(VerificationCooldownException.class);

            verify(tokenRepository, never()).save(any());
            verify(brevoClient, never()).sendTemplateEmail(any(), any(), anyLong(), any());
        }

        @Test
        void allowsSendWhenCooldownHasElapsed() {
            UserAccount user = emailUser();
            when(userAccountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
            when(tokenRepository.findLatestCreatedAtForUser(1L))
                    .thenReturn(Optional.of(Instant.now().minusSeconds(61)));

            service.sendResetEmail("test@example.com", Locale.ENGLISH);

            verify(tokenRepository).save(any());
            verify(brevoClient).sendTemplateEmail(any(), any(), anyLong(), any());
        }
    }

    @Nested
    class ResetPassword {

        @Test
        void updatesPasswordAndIncrementsTokenVersionForEmailUser() {
            String plainToken = "valid-token";
            String tokenHash = TokenHashUtil.hashToken(plainToken);
            UserAccount user = emailUser();
            int initialTokenVersion = user.getTokenVersion();
            PasswordResetToken token = PasswordResetToken.builder()
                    .tokenHash(tokenHash)
                    .user(user)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
            when(userAccountRepository.save(user)).thenReturn(user);

            service.resetPassword(plainToken, "new-password");

            assertThat(user.getPasswordHash()).isEqualTo("new-hash");
            assertThat(user.getTokenVersion()).isEqualTo(initialTokenVersion + 1);
            assertThat(user.getProviders()).containsExactly(AuthProvider.EMAIL);
            assertThat(token.getUsedAt()).isNotNull();
            verify(userAccountRepository).save(user);
        }

        @Test
        void addsEmailProviderForGoogleOnlyUser() {
            String plainToken = "valid-token";
            String tokenHash = TokenHashUtil.hashToken(plainToken);
            UserAccount user = googleOnlyUser();
            int initialTokenVersion = user.getTokenVersion();
            PasswordResetToken token = PasswordResetToken.builder()
                    .tokenHash(tokenHash)
                    .user(user)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
            when(passwordEncoder.encode("new-password")).thenReturn("new-hash");
            when(userAccountRepository.save(user)).thenReturn(user);

            service.resetPassword(plainToken, "new-password");

            assertThat(user.getPasswordHash()).isEqualTo("new-hash");
            assertThat(user.getProviders()).containsExactlyInAnyOrder(AuthProvider.GOOGLE, AuthProvider.EMAIL);
            assertThat(user.getTokenVersion()).isEqualTo(initialTokenVersion + 1);
            assertThat(token.getUsedAt()).isNotNull();
        }

        @Test
        void throwsOnNonexistentToken() {
            String plainToken = "nonexistent";
            String tokenHash = TokenHashUtil.hashToken(plainToken);
            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.resetPassword(plainToken, "new-password"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("Invalid");

            verify(userAccountRepository, never()).save(any());
        }

        @Test
        void throwsOnAlreadyUsedToken() {
            String plainToken = "used-token";
            String tokenHash = TokenHashUtil.hashToken(plainToken);
            PasswordResetToken token = PasswordResetToken.builder()
                    .tokenHash(tokenHash)
                    .user(emailUser())
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .usedAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.resetPassword(plainToken, "new-password"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("already been used");

            verify(userAccountRepository, never()).save(any());
        }

        @Test
        void throwsOnExpiredToken() {
            String plainToken = "expired-token";
            String tokenHash = TokenHashUtil.hashToken(plainToken);
            PasswordResetToken token = PasswordResetToken.builder()
                    .tokenHash(tokenHash)
                    .user(emailUser())
                    .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                    .build();

            when(tokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

            assertThatThrownBy(() -> service.resetPassword(plainToken, "new-password"))
                    .isInstanceOf(InvalidTokenException.class)
                    .hasMessageContaining("expired");

            verify(userAccountRepository, never()).save(any());
        }
    }
}
