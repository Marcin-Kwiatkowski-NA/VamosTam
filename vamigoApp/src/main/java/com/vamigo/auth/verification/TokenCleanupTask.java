package com.vamigo.auth.verification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class TokenCleanupTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(TokenCleanupTask.class);

    private final EmailVerificationTokenRepository emailTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public TokenCleanupTask(EmailVerificationTokenRepository emailTokenRepository,
                            PasswordResetTokenRepository passwordResetTokenRepository) {
        this.emailTokenRepository = emailTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    @Scheduled(cron = "${auth.token-cleanup-cron}")
    @Transactional
    public void deleteExpiredTokens() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        emailTokenRepository.deleteByExpiresAtBefore(cutoff);
        passwordResetTokenRepository.deleteByExpiresAtBefore(cutoff);
        LOGGER.info("Cleaned up expired tokens older than {}", cutoff);
    }
}
