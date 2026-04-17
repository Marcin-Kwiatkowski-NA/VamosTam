package com.vamigo.auth.verification;

import com.vamigo.AbstractIntegrationTest;
import com.vamigo.user.AccountStatus;
import com.vamigo.user.UserAccount;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
    @DisplayName("Returns the verification token whose stored hash matches")
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
    @DisplayName("Returns empty when no verification token matches the hash")
    void findByTokenHash_returnsEmptyForNonexistentHash() {
        Optional<EmailVerificationToken> found = tokenRepository.findByTokenHash("nonexistent");

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("Marks every unused verification token for the user as used")
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
    @DisplayName("Returns a creation timestamp when the user has at least one verification token")
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
    @DisplayName("Returns empty when the user has no verification tokens")
    void findLatestCreatedAtForUser_returnsEmptyWhenNoTokens() {
        Optional<Instant> latest = tokenRepository.findLatestCreatedAtForUser(user.getId());

        assertThat(latest).isEmpty();
    }

    @Nested
    @DisplayName("Delete verification tokens expired before a cutoff")
    class DeleteByExpiresAtBeforeTests {

        @Test
        @DisplayName("Removes tokens whose expiry is strictly before the cutoff")
        void removesTokensExpiredBeforeCutoff() {
            EmailVerificationToken expired = EmailVerificationToken.builder()
                    .tokenHash(TokenHashUtil.hashToken("expired"))
                    .user(user)
                    .expiresAt(Instant.now().minus(2, ChronoUnit.HOURS))
                    .build();
            EmailVerificationToken fresh = EmailVerificationToken.builder()
                    .tokenHash(TokenHashUtil.hashToken("fresh"))
                    .user(user)
                    .expiresAt(Instant.now().plus(2, ChronoUnit.HOURS))
                    .build();
            entityManager.persist(expired);
            entityManager.persist(fresh);
            entityManager.flush();
            Long expiredId = expired.getId();
            Long freshId = fresh.getId();
            entityManager.clear();

            tokenRepository.deleteByExpiresAtBefore(Instant.now().minus(1, ChronoUnit.HOURS));
            entityManager.flush();
            entityManager.clear();

            assertThat(entityManager.find(EmailVerificationToken.class, expiredId)).isNull();
            assertThat(entityManager.find(EmailVerificationToken.class, freshId)).isNotNull();
        }

        @Test
        @DisplayName("Keeps tokens whose expiry equals the cutoff instant")
        void leavesTokensExactlyAtCutoff() {
            Instant cutoff = Instant.now();
            EmailVerificationToken atCutoff = EmailVerificationToken.builder()
                    .tokenHash(TokenHashUtil.hashToken("at-cutoff"))
                    .user(user)
                    .expiresAt(cutoff)
                    .build();
            entityManager.persist(atCutoff);
            entityManager.flush();
            Long atCutoffId = atCutoff.getId();
            entityManager.clear();

            tokenRepository.deleteByExpiresAtBefore(cutoff);
            entityManager.flush();
            entityManager.clear();

            assertThat(entityManager.find(EmailVerificationToken.class, atCutoffId)).isNotNull();
        }
    }

    @Nested
    @DisplayName("Delete every verification token for a user")
    class DeleteByUserIdTests {

        @Test
        @DisplayName("Removes every verification token belonging to the user")
        void removesAllTokensForUser() {
            EmailVerificationToken t1 = EmailVerificationToken.builder()
                    .tokenHash(TokenHashUtil.hashToken("u1-t1"))
                    .user(user)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();
            EmailVerificationToken t2 = EmailVerificationToken.builder()
                    .tokenHash(TokenHashUtil.hashToken("u1-t2"))
                    .user(user)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();
            entityManager.persist(t1);
            entityManager.persist(t2);
            entityManager.flush();
            Long id1 = t1.getId();
            Long id2 = t2.getId();
            entityManager.clear();

            tokenRepository.deleteByUserId(user.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(entityManager.find(EmailVerificationToken.class, id1)).isNull();
            assertThat(entityManager.find(EmailVerificationToken.class, id2)).isNull();
        }

        @Test
        @DisplayName("Leaves tokens that belong to other users untouched")
        void leavesTokensForOtherUsers() {
            UserAccount otherUser = UserAccount.builder()
                    .email("other@example.com")
                    .status(AccountStatus.ACTIVE)
                    .roles(Set.of())
                    .providers(Set.of())
                    .build();
            entityManager.persist(otherUser);
            EmailVerificationToken ownToken = EmailVerificationToken.builder()
                    .tokenHash(TokenHashUtil.hashToken("own"))
                    .user(user)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();
            EmailVerificationToken otherToken = EmailVerificationToken.builder()
                    .tokenHash(TokenHashUtil.hashToken("other"))
                    .user(otherUser)
                    .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                    .build();
            entityManager.persist(ownToken);
            entityManager.persist(otherToken);
            entityManager.flush();
            Long ownId = ownToken.getId();
            Long otherId = otherToken.getId();
            entityManager.clear();

            tokenRepository.deleteByUserId(user.getId());
            entityManager.flush();
            entityManager.clear();

            assertThat(entityManager.find(EmailVerificationToken.class, ownId)).isNull();
            assertThat(entityManager.find(EmailVerificationToken.class, otherId)).isNotNull();
        }
    }
}
