package com.blablatwo.auth.verification;

import com.blablatwo.AbstractIntegrationTest;
import com.blablatwo.user.AccountStatus;
import com.blablatwo.user.UserAccount;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class EmailVerificationTokenRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private EmailVerificationTokenRepository tokenRepository;

    @Autowired
    private EntityManager entityManager;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        user = UserAccount.builder()
                .email("verify-test@example.com")
                .status(AccountStatus.ACTIVE)
                .roles(Set.of())
                .providers(Set.of())
                .build();
        entityManager.persist(user);
        entityManager.flush();
    }

    @Test
    void findByTokenHash_returnsCorrectToken() {
        String tokenHash = TokenHashUtil.hashToken("test-token");
        EmailVerificationToken token = EmailVerificationToken.builder()
                .tokenHash(tokenHash)
                .user(user)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        entityManager.persist(token);
        entityManager.flush();

        Optional<EmailVerificationToken> found = tokenRepository.findByTokenHash(tokenHash);

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void findByTokenHash_returnsEmptyForNonexistentHash() {
        Optional<EmailVerificationToken> found = tokenRepository.findByTokenHash("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    void invalidateAllForUser_marksAllUnusedTokensAsUsed() {
        EmailVerificationToken token1 = EmailVerificationToken.builder()
                .tokenHash(TokenHashUtil.hashToken("token-1"))
                .user(user)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        EmailVerificationToken token2 = EmailVerificationToken.builder()
                .tokenHash(TokenHashUtil.hashToken("token-2"))
                .user(user)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        entityManager.persist(token1);
        entityManager.persist(token2);
        entityManager.flush();

        tokenRepository.invalidateAllForUser(user.getId());
        entityManager.clear();

        EmailVerificationToken reloaded1 = entityManager.find(EmailVerificationToken.class, token1.getId());
        EmailVerificationToken reloaded2 = entityManager.find(EmailVerificationToken.class, token2.getId());

        assertThat(reloaded1.getUsedAt()).isNotNull();
        assertThat(reloaded2.getUsedAt()).isNotNull();
    }

    @Test
    void findLatestCreatedAtForUser_returnsLatestTimestamp() {
        EmailVerificationToken older = EmailVerificationToken.builder()
                .tokenHash(TokenHashUtil.hashToken("older"))
                .user(user)
                .expiresAt(Instant.now().plus(24, ChronoUnit.HOURS))
                .build();
        entityManager.persist(older);
        entityManager.flush();

        Optional<Instant> latest = tokenRepository.findLatestCreatedAtForUser(user.getId());

        assertThat(latest).isPresent();
    }

    @Test
    void findLatestCreatedAtForUser_returnsEmptyWhenNoTokens() {
        Optional<Instant> latest = tokenRepository.findLatestCreatedAtForUser(user.getId());

        assertThat(latest).isEmpty();
    }
}
