package com.vamigo.user;

import com.vamigo.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static com.vamigo.util.TestFixtures.anActiveUserAccount;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserProfileRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserProfileRepository repository;

    @Nested
    @DisplayName("Insert profile if it does not exist")
    class InsertProfileIfNotExistsTests {

        @Test
        @DisplayName("Creates a new profile with zeroed stats for a fresh user")
        void createsProfileWithZeroedStats() {
            UserAccount user = em.persistAndFlush(anActiveUserAccount().build());
            em.clear();

            repository.insertProfileIfNotExists(user.getId(), "Display Name");
            em.clear();

            UserProfile loaded = em.find(UserProfile.class, user.getId());
            assertThat(loaded).isNotNull();
            assertThat(loaded.getDisplayName()).isEqualTo("Display Name");
            assertThat(loaded.getStats().getRidesGiven()).isZero();
            assertThat(loaded.getStats().getRidesTaken()).isZero();
            assertThat(loaded.getStats().getRatingSum()).isZero();
            assertThat(loaded.getStats().getRatingCount()).isZero();
        }

        @Test
        @DisplayName("Keeps the existing profile unchanged when one already exists")
        void isIdempotentWhenProfileExists() {
            UserAccount user = em.persistAndFlush(anActiveUserAccount().build());
            em.clear();

            repository.insertProfileIfNotExists(user.getId(), "First");
            em.clear();
            repository.insertProfileIfNotExists(user.getId(), "Changed");
            em.clear();

            UserProfile loaded = em.find(UserProfile.class, user.getId());
            assertThat(loaded.getDisplayName()).isEqualTo("First");
        }
    }
}
