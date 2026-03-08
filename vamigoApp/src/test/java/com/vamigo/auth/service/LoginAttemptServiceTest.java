package com.vamigo.auth.service;

import com.vamigo.auth.AuthSecurityProperties;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    private static final String EMAIL = "test@example.com";
    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    @Mock
    private UserAccountRepository userAccountRepository;

    private LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        var properties = new AuthSecurityProperties(
                new AuthSecurityProperties.RateLimit(20),
                new AuthSecurityProperties.AccountLock(MAX_ATTEMPTS, LOCK_MINUTES)
        );
        loginAttemptService = new LoginAttemptService(userAccountRepository, properties);
    }

    @Test
    @DisplayName("recordFailedAttempt increments counter on existing account")
    void recordFailedAttempt_incrementsCounter() {
        UserAccount account = UserAccount.builder().email(EMAIL).build();
        when(userAccountRepository.findByEmail(EMAIL)).thenReturn(Optional.of(account));

        loginAttemptService.recordFailedAttempt(EMAIL);

        assertThat(account.getFailedLoginAttempts()).isEqualTo(1);
        verify(userAccountRepository).save(account);
    }

    @Test
    @DisplayName("recordFailedAttempt locks account at threshold")
    void recordFailedAttempt_locksAtThreshold() {
        UserAccount account = UserAccount.builder().email(EMAIL).failedLoginAttempts(MAX_ATTEMPTS - 1).build();
        when(userAccountRepository.findByEmail(EMAIL)).thenReturn(Optional.of(account));

        loginAttemptService.recordFailedAttempt(EMAIL);

        assertThat(account.getFailedLoginAttempts()).isEqualTo(MAX_ATTEMPTS);
        assertThat(account.getLockedUntil()).isNotNull();
        assertThat(account.isTemporarilyLocked()).isTrue();
    }

    @Test
    @DisplayName("recordFailedAttempt does nothing for unknown email")
    void recordFailedAttempt_unknownEmail_doesNothing() {
        when(userAccountRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        loginAttemptService.recordFailedAttempt("unknown@example.com");

        verify(userAccountRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("resetAttempts clears counter and lock")
    void resetAttempts_clearsCounterAndLock() {
        UserAccount account = UserAccount.builder().email(EMAIL).failedLoginAttempts(3).build();
        when(userAccountRepository.findByEmail(EMAIL)).thenReturn(Optional.of(account));

        loginAttemptService.resetAttempts(EMAIL);

        assertThat(account.getFailedLoginAttempts()).isZero();
        assertThat(account.getLockedUntil()).isNull();
        verify(userAccountRepository).save(account);
    }

    @Test
    @DisplayName("resetAttempts is a no-op when counter is already zero")
    void resetAttempts_noOpWhenAlreadyZero() {
        UserAccount account = UserAccount.builder().email(EMAIL).build();
        when(userAccountRepository.findByEmail(EMAIL)).thenReturn(Optional.of(account));

        loginAttemptService.resetAttempts(EMAIL);

        verify(userAccountRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
