package com.vamigo.auth.event;

import com.vamigo.auth.service.LoginAttemptService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import javax.crypto.SecretKey;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationEventListenerTest {

    @Mock
    private LoginAttemptService loginAttemptService;

    @InjectMocks
    private AuthenticationEventListener listener;

    @Test
    @DisplayName("onFailure records failed attempt with email from event")
    void onFailure_recordsFailedAttempt() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user@example.com");
        var event = new AuthenticationFailureBadCredentialsEvent(
                auth, new org.springframework.security.authentication.BadCredentialsException("bad"));

        listener.onFailure(event);

        verify(loginAttemptService).recordFailedAttempt("user@example.com");
    }

    @Test
    @DisplayName("onSuccess resets attempts for password login")
    void onSuccess_resetsAttemptsForPasswordLogin() {
        var auth = new UsernamePasswordAuthenticationToken("user@example.com", null);
        var event = new AuthenticationSuccessEvent(auth);

        listener.onSuccess(event);

        verify(loginAttemptService).resetAttempts("user@example.com");
    }

    @Test
    @DisplayName("onSuccess ignores non-password authentication (e.g. JWT)")
    void onSuccess_ignoresJwtAuthentication() {
        Authentication auth = mock(JwtAuthenticationToken.class);
        var event = new AuthenticationSuccessEvent(auth);

        listener.onSuccess(event);

        verify(loginAttemptService, never()).resetAttempts(org.mockito.ArgumentMatchers.any());
    }
}
