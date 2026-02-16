package com.blablatwo.auth.event;

import com.blablatwo.auth.verification.EmailVerificationService;
import com.blablatwo.email.EmailSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class RegistrationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationListener.class);

    private final EmailVerificationService emailVerificationService;

    public RegistrationListener(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @Async
    @EventListener
    public void handleRegistration(OnRegistrationCompleteEvent event) {
        try {
            emailVerificationService.sendVerificationEmail(event.user(), event.locale());
        } catch (EmailSendException e) {
            LOGGER.error("Failed to send verification email to {}: {}",
                    event.user().getEmail(), e.getMessage());
        }
    }
}
