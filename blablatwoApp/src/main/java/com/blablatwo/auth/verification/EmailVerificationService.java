package com.blablatwo.auth.verification;

import com.blablatwo.auth.exception.InvalidTokenException;
import com.blablatwo.auth.exception.VerificationCooldownException;
import com.blablatwo.email.BrevoClient;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final BrevoClient brevoClient;
    private final int tokenExpiryHours;
    private final int cooldownSeconds;
    private final String baseUrl;
    private final long verificationTemplateId;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                    UserAccountRepository userAccountRepository,
                                    BrevoClient brevoClient,
                                    @Value("${app.email-verification.token-expiry-hours}") int tokenExpiryHours,
                                    @Value("${app.email-verification.cooldown-seconds}") int cooldownSeconds,
                                    @Value("${app.email-verification.base-url}") String baseUrl,
                                    @Value("${brevo.verification-template-id}") long verificationTemplateId) {
        this.tokenRepository = tokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.brevoClient = brevoClient;
        this.tokenExpiryHours = tokenExpiryHours;
        this.cooldownSeconds = cooldownSeconds;
        this.baseUrl = baseUrl;
        this.verificationTemplateId = verificationTemplateId;
    }

    @Transactional
    public void sendVerificationEmail(UserAccount user) {
        checkCooldown(user.getId());

        tokenRepository.invalidateAllForUser(user.getId());

        String plainToken = TokenHashUtil.generateToken();
        String tokenHash = TokenHashUtil.hashToken(plainToken);

        EmailVerificationToken token = EmailVerificationToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(Instant.now().plus(tokenExpiryHours, ChronoUnit.HOURS))
                .build();
        tokenRepository.save(token);

        String verificationLink = baseUrl + "/auth/verify-email?token=" + plainToken;
        String userName = user.getEmail().split("@")[0];

        brevoClient.sendTemplateEmail(
                user.getEmail(),
                userName,
                verificationTemplateId,
                Map.of(
                        "VERIFICATION_URL", verificationLink,
                        "USER_NAME", userName
                )
        );
    }

    @Transactional
    public void verifyEmail(String plainToken) {
        String tokenHash = TokenHashUtil.hashToken(plainToken);

        EmailVerificationToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (token.isUsed()) {
            throw new InvalidTokenException("Verification token has already been used");
        }

        if (token.isExpired()) {
            throw new InvalidTokenException("Verification token has expired");
        }

        token.setUsedAt(Instant.now());

        UserAccount user = token.getUser();
        user.setEmailVerifiedAt(Instant.now());
        userAccountRepository.save(user);
    }

    private void checkCooldown(Long userId) {
        tokenRepository.findLatestCreatedAtForUser(userId)
                .ifPresent(lastCreatedAt -> {
                    Instant cooldownEnd = lastCreatedAt.plusSeconds(cooldownSeconds);
                    if (Instant.now().isBefore(cooldownEnd)) {
                        throw new VerificationCooldownException(cooldownSeconds);
                    }
                });
    }
}
