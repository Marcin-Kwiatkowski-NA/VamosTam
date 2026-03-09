package com.vamigo.auth.verification;

import com.vamigo.auth.AuthProvider;
import com.vamigo.auth.exception.InvalidTokenException;
import com.vamigo.auth.exception.VerificationCooldownException;
import com.vamigo.email.BrevoClient;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;

@Service
public class PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenRepository tokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrevoClient brevoClient;
    private final int tokenExpiryHours;
    private final int cooldownSeconds;
    private final String resetUrl;
    private final long resetTemplateId;
    private final long resetTemplateIdPl;

    public PasswordResetService(PasswordResetTokenRepository tokenRepository,
                                UserAccountRepository userAccountRepository,
                                PasswordEncoder passwordEncoder,
                                BrevoClient brevoClient,
                                @Value("${app.password-reset.token-expiry-hours:1}") int tokenExpiryHours,
                                @Value("${app.password-reset.cooldown-seconds:60}") int cooldownSeconds,
                                @Value("${app.password-reset.reset-url}") String resetUrl,
                                @Value("${brevo.password-reset-template-id}") long resetTemplateId,
                                @Value("${brevo.password-reset-template-id-pl}") long resetTemplateIdPl) {
        this.tokenRepository = tokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.brevoClient = brevoClient;
        this.tokenExpiryHours = tokenExpiryHours;
        this.cooldownSeconds = cooldownSeconds;
        this.resetUrl = resetUrl;
        this.resetTemplateId = resetTemplateId;
        this.resetTemplateIdPl = resetTemplateIdPl;
    }

    @Transactional
    public void sendResetEmail(String email, Locale locale) {
        var optionalUser = userAccountRepository.findByEmail(email.toLowerCase());
        if (optionalUser.isEmpty()) {
            LOGGER.debug("Password reset requested for non-existent email: {}", email);
            return;
        }

        UserAccount user = optionalUser.get();

        // Skip if user has no password (Google-only account)
        if (!user.hasProvider(AuthProvider.EMAIL)) {
            LOGGER.debug("Password reset requested for Google-only account: {}", email);
            return;
        }

        checkCooldown(user.getId());

        tokenRepository.invalidateAllForUser(user.getId());

        String plainToken = TokenHashUtil.generateToken();
        String tokenHash = TokenHashUtil.hashToken(plainToken);

        PasswordResetToken token = PasswordResetToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(Instant.now().plus(tokenExpiryHours, ChronoUnit.HOURS))
                .build();
        tokenRepository.save(token);

        String resetLink = resetUrl + "?token=" + plainToken;
        String userName = user.getEmail().split("@")[0];

        long templateId = "pl".equals(locale.getLanguage()) ? resetTemplateIdPl : resetTemplateId;

        brevoClient.sendTemplateEmail(
                user.getEmail(),
                userName,
                templateId,
                Map.of(
                        "RESET_URL", resetLink,
                        "USER_NAME", userName
                )
        );
    }

    @Transactional
    public UserAccount resetPassword(String plainToken, String newPassword) {
        String tokenHash = TokenHashUtil.hashToken(plainToken);

        PasswordResetToken token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        if (token.isUsed()) {
            throw new InvalidTokenException("Password reset token has already been used");
        }

        if (token.isExpired()) {
            throw new InvalidTokenException("Password reset token has expired");
        }

        token.setUsedAt(Instant.now());

        UserAccount user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.incrementTokenVersion();
        return userAccountRepository.save(user);
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
