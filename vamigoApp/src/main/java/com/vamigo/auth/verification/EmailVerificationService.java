package com.vamigo.auth.verification;

import com.vamigo.auth.exception.InvalidTokenException;
import com.vamigo.auth.exception.VerificationCooldownException;
import com.vamigo.email.BrevoClient;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
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
    private final long verificationTemplateIdPl;

    public EmailVerificationService(EmailVerificationTokenRepository tokenRepository,
                                    UserAccountRepository userAccountRepository,
                                    BrevoClient brevoClient,
                                    @Value("${app.email-verification.token-expiry-hours}") int tokenExpiryHours,
                                    @Value("${app.email-verification.cooldown-seconds}") int cooldownSeconds,
                                    @Value("${app.email-verification.base-url}") String baseUrl,
                                    @Value("${brevo.verification-template-id}") long verificationTemplateId,
                                    @Value("${brevo.verification-template-id-pl}") long verificationTemplateIdPl) {
        this.tokenRepository = tokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.brevoClient = brevoClient;
        this.tokenExpiryHours = tokenExpiryHours;
        this.cooldownSeconds = cooldownSeconds;
        this.baseUrl = baseUrl;
        this.verificationTemplateId = verificationTemplateId;
        this.verificationTemplateIdPl = verificationTemplateIdPl;
    }

    @Transactional
    public void sendVerificationEmail(UserAccount user, Locale locale) {
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

        long templateId = "pl".equals(locale.getLanguage()) ? verificationTemplateIdPl : verificationTemplateId;

        brevoClient.sendTemplateEmail(
                user.getEmail(),
                userName,
                templateId,
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

        Instant now = Instant.now();
        token.markUsed(now);

        UserAccount user = token.getUser();
        user.markEmailVerified(now);
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
