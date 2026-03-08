package com.vamigo.auth.event;

import com.vamigo.auth.service.LoginAttemptService;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationEventListener {

    private final LoginAttemptService loginAttemptService;

    public AuthenticationEventListener(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }

    @EventListener
    public void onFailure(AuthenticationFailureBadCredentialsEvent event) {
        String email = event.getAuthentication().getName();
        loginAttemptService.recordFailedAttempt(email);
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        if (event.getAuthentication() instanceof UsernamePasswordAuthenticationToken) {
            String email = event.getAuthentication().getName();
            loginAttemptService.resetAttempts(email);
        }
    }
}
