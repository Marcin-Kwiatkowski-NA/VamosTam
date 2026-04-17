package com.vamigo.notification;

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
import java.util.List;
import java.util.Optional;

import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class DeviceTokenRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private DeviceTokenRepository repository;

    private UserAccount user;
    private UserAccount otherUser;

    @BeforeEach
    void setUp() {
        user = em.persistAndFlush(anActiveUserAccount().build());
        otherUser = em.persistAndFlush(anActiveUserAccount().email("other@example.com").build());
    }

    @Nested
    @DisplayName("List device tokens owned by a user")
    class FindByUserIdTests {

        @Test
        @DisplayName("Returns only the tokens that belong to the given user")
        void returnsTokensForUser() {
            em.persistAndFlush(aDeviceToken(user).id(null).token("t-1").createdAt(Instant.now()).build());
            em.persistAndFlush(aDeviceToken(user).id(null).token("t-2").createdAt(Instant.now()).build());
            em.persistAndFlush(aDeviceToken(otherUser).id(null).token("t-3").createdAt(Instant.now()).build());
            em.clear();

            List<DeviceToken> tokens = repository.findByUserId(user.getId());

            assertThat(tokens).hasSize(2)
                    .extracting(DeviceToken::getToken).containsExactlyInAnyOrder("t-1", "t-2");
        }

        @Test
        @DisplayName("Returns empty list when the user has no registered devices")
        void returnsEmptyWhenUserHasNoTokens() {
            assertThat(repository.findByUserId(user.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Lookup device token by its token string")
    class FindByTokenTests {

        @Test
        @DisplayName("Returns the device token when one is registered for the given FCM token")
        void returnsTokenWhenPresent() {
            DeviceToken persisted = em.persistAndFlush(
                    aDeviceToken(user).id(null).token("unique-token").createdAt(Instant.now()).build());
            em.clear();

            Optional<DeviceToken> found = repository.findByToken("unique-token");

            assertThat(found).isPresent()
                    .get().extracting(DeviceToken::getId).isEqualTo(persisted.getId());
        }

        @Test
        @DisplayName("Returns empty when no device is registered with that token string")
        void returnsEmptyWhenAbsent() {
            assertThat(repository.findByToken("nonexistent")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Delete device token scoped to owner")
    class DeleteByUserIdAndIdTests {

        @Test
        @DisplayName("Deletes the device token when the id and owning user both match")
        void deletesWhenOwnerMatches() {
            DeviceToken token = em.persistAndFlush(
                    aDeviceToken(user).id(null).token("owned").createdAt(Instant.now()).build());
            em.clear();

            repository.deleteByUserIdAndId(user.getId(), token.getId());
            em.flush();
            em.clear();

            assertThat(em.find(DeviceToken.class, token.getId())).isNull();
        }

        @Test
        @DisplayName("Keeps the device token when another user attempts to delete it")
        void doesNotDeleteWhenOwnerDoesNotMatch() {
            DeviceToken token = em.persistAndFlush(
                    aDeviceToken(user).id(null).token("owned").createdAt(Instant.now()).build());
            em.clear();

            repository.deleteByUserIdAndId(otherUser.getId(), token.getId());
            em.flush();
            em.clear();

            assertThat(em.find(DeviceToken.class, token.getId())).isNotNull();
        }
    }
}
