package com.vamigo.auth.verification;

import com.vamigo.AbstractIntegrationTest;
import com.vamigo.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class PasswordResetTokenRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PasswordResetTokenRepository repository;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        user = em.persistAndFlush(anActiveUserAccount().build());
    }

    @Nested
    @DisplayName("Lookup reset token by hash")
    class FindByTokenHashTests {

        @Test
        @DisplayName("Returns token when the stored hash matches")
        void returnsTokenWhenHashMatches() {
            String hash = TokenHashUtil.hashToken("plain-text-token");
            PasswordResetToken token = em.persistAndFlush(
                    aPasswordResetToken(user).tokenHash(hash).build());
            em.clear();

            Optional<PasswordResetToken> found = repository.findByTokenHash(hash);

            assertThat(found).isPresent()
                    .get().extracting(PasswordResetToken::getId).isEqualTo(token.getId());
        }

        @Test
        @DisplayName("Returns empty when no token matches the hash")
        void returnsEmptyWhenAbsent() {
            assertThat(repository.findByTokenHash("nonexistent")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Invalidate every outstanding token for a user")
    class InvalidateAllForUserTests {

        @Test
        @DisplayName("Marks every unused token for the user as used")
        void marksAllUnusedTokensAsUsed() {
            PasswordResetToken t1 = em.persistAndFlush(aPasswordResetToken(user).build());
            PasswordResetToken t2 = em.persistAndFlush(aPasswordResetToken(user).build());
            em.clear();

            repository.invalidateAllForUser(user.getId());
            em.clear();

            assertThat(em.find(PasswordResetToken.class, t1.getId()).getUsedAt()).isNotNull();
            assertThat(em.find(PasswordResetToken.class, t2.getId()).getUsedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Latest reset-token timestamp for a user")
    class FindLatestCreatedAtTests {

        @Test
        @DisplayName("Returns the creation timestamp when the user has at least one token")
        void returnsTimestampWhenTokensExist() {
            em.persistAndFlush(aPasswordResetToken(user).build());
            em.clear();

            assertThat(repository.findLatestCreatedAtForUser(user.getId())).isPresent();
        }

        @Test
        @DisplayName("Returns empty when the user has never been issued a token")
        void returnsEmptyWhenNoTokens() {
            assertThat(repository.findLatestCreatedAtForUser(user.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Delete reset tokens expired before a cutoff")
    class DeleteExpiredTests {

        @Test
        @DisplayName("Removes tokens whose expiry is before the cutoff and keeps the rest")
        void deletesTokensWithExpiryBeforeCutoff() {
            PasswordResetToken expired = em.persistAndFlush(
                    aPasswordResetToken(user).expiresAt(Instant.now().minus(2, ChronoUnit.DAYS)).build());
            PasswordResetToken active = em.persistAndFlush(
                    aPasswordResetToken(user).expiresAt(Instant.now().plus(1, ChronoUnit.DAYS)).build());
            em.clear();

            repository.deleteByExpiresAtBefore(Instant.now());
            em.flush();
            em.clear();

            assertThat(em.find(PasswordResetToken.class, expired.getId())).isNull();
            assertThat(em.find(PasswordResetToken.class, active.getId())).isNotNull();
        }
    }

    @Nested
    @DisplayName("Delete every reset token for a user")
    class DeleteByUserIdTests {

        @Test
        @DisplayName("Removes only tokens that belong to the target user")
        void deletesAllTokensForUser() {
            UserAccount other = em.persistAndFlush(anActiveUserAccount().email("other@example.com").build());
            PasswordResetToken own = em.persistAndFlush(aPasswordResetToken(user).build());
            PasswordResetToken foreign = em.persistAndFlush(aPasswordResetToken(other).build());
            em.clear();

            repository.deleteByUserId(user.getId());
            em.flush();
            em.clear();

            assertThat(em.find(PasswordResetToken.class, own.getId())).isNull();
            assertThat(em.find(PasswordResetToken.class, foreign.getId())).isNotNull();
        }
    }
}
