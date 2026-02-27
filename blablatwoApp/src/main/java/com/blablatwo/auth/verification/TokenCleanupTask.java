package com.blablatwo.auth.verification;

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

    private final EmailVerificationTokenRepository tokenRepository;

    public TokenCleanupTask(EmailVerificationTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Scheduled(cron = "${auth.token-cleanup-cron}")
    @Transactional
    public void deleteExpiredTokens() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        tokenRepository.deleteByExpiresAtBefore(cutoff);
        LOGGER.info("Cleaned up expired verification tokens older than {}", cutoff);
    }
}
