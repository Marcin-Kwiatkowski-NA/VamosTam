package com.vamigo.auth.event;

import com.vamigo.auth.verification.EmailVerificationService;
import com.vamigo.email.EmailSendException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RegistrationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationListener.class);

    private final EmailVerificationService emailVerificationService;

    public RegistrationListener(EmailVerificationService emailVerificationService) {
        this.emailVerificationService = emailVerificationService;
    }

    @Async
    @TransactionalEventListener
    public void handleRegistration(OnRegistrationCompleteEvent event) {
        try {
            emailVerificationService.sendVerificationEmail(event.user(), event.locale());
        } catch (EmailSendException e) {
            LOGGER.error("Failed to send verification email to {}: {}",
                    event.user().getEmail(), e.getMessage());
        }
    }
}
