package com.vamigo.searchalert;

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

import java.util.Optional;

import static com.vamigo.util.TestFixtures.anActiveUserAccount;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class NotificationPreferenceRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private NotificationPreferenceRepository repository;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        user = em.persistAndFlush(anActiveUserAccount().build());
    }

    @Nested
    @DisplayName("Look up notification preference by unsubscribe token")
    class FindByUnsubscribeTokenTests {

        @Test
        @DisplayName("Returns the preference when a stored token matches the given value")
        void returnsPrefWhenTokenMatches() {
            NotificationPreference pref = NotificationPreference.builder()
                    .user(user).unsubscribeToken("known-token").build();
            em.persistAndFlush(pref);
            em.clear();

            Optional<NotificationPreference> found = repository.findByUnsubscribeToken("known-token");

            assertThat(found).isPresent()
                    .get().extracting(NotificationPreference::getUserId).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("Returns empty when no preference carries the supplied token")
        void returnsEmptyWhenAbsent() {
            assertThat(repository.findByUnsubscribeToken("nonexistent")).isEmpty();
        }
    }
}
