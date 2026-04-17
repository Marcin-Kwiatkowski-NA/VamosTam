package com.vamigo.user;

import com.vamigo.AbstractIntegrationTest;
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
class UserAccountRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserAccountRepository repository;

    @Nested
    @DisplayName("Lookup account by email")
    class FindByEmailTests {

        @Test
        @DisplayName("Returns account when email matches an existing user")
        void returnsAccountWhenEmailMatches() {
            UserAccount user = em.persistAndFlush(anActiveUserAccount().email("found@example.com").build());
            em.clear();

            Optional<UserAccount> found = repository.findByEmail("found@example.com");

            assertThat(found).isPresent()
                    .get().extracting(UserAccount::getId).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("Returns empty when no account exists for the given email")
        void returnsEmptyWhenAbsent() {
            assertThat(repository.findByEmail("nobody@example.com")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Lookup account by Google id")
    class FindByGoogleIdTests {

        @Test
        @DisplayName("Returns account when the Google id is registered")
        void returnsAccountByGoogleId() {
            em.persistAndFlush(anActiveUserAccount().email("g@example.com").googleId("g-123").build());
            em.clear();

            assertThat(repository.findByGoogleId("g-123")).isPresent();
        }

        @Test
        @DisplayName("Returns empty when no account matches the Google id")
        void returnsEmptyWhenAbsent() {
            assertThat(repository.findByGoogleId("missing")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Check if email is registered")
    class ExistsByEmailTests {

        @Test
        @DisplayName("Returns true for saved emails and false for unknown ones")
        void reflectsInsertedAccount() {
            em.persistAndFlush(anActiveUserAccount().email("exists@example.com").build());
            em.clear();

            assertThat(repository.existsByEmail("exists@example.com")).isTrue();
            assertThat(repository.existsByEmail("nope@example.com")).isFalse();
        }
    }

    @Nested
    @DisplayName("Insert system user if missing")
    class InsertSystemUserTests {

        @Test
        @DisplayName("Inserts the system user row when the id is not yet present")
        void insertsRowWhenMissing() {
            repository.insertSystemUserIfNotExists(9999L, "system@example.com", "ACTIVE");
            em.clear();

            UserAccount loaded = em.find(UserAccount.class, 9999L);
            assertThat(loaded).isNotNull();
            assertThat(loaded.getEmail()).isEqualTo("system@example.com");
            assertThat(loaded.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        }

        @Test
        @DisplayName("Leaves the existing system user untouched on repeated calls")
        void isIdempotentWhenRowExists() {
            repository.insertSystemUserIfNotExists(9999L, "system@example.com", "ACTIVE");
            em.clear();

            repository.insertSystemUserIfNotExists(9999L, "changed@example.com", "ACTIVE");
            em.clear();

            UserAccount loaded = em.find(UserAccount.class, 9999L);
            assertThat(loaded.getEmail()).isEqualTo("system@example.com");
        }
    }

    @Nested
    @DisplayName("Insert role for user if missing")
    class InsertRoleTests {

        @Test
        @DisplayName("Assigns the role to the user when they do not yet have it")
        void insertsRoleForUser() {
            UserAccount user = em.persistAndFlush(anActiveUserAccount().email("role@example.com").build());
            em.clear();

            repository.insertRoleIfNotExists(user.getId(), "ADMIN");
            em.clear();

            UserAccount reloaded = em.find(UserAccount.class, user.getId());
            assertThat(reloaded.getRoles()).contains(Role.ADMIN);
        }

        @Test
        @DisplayName("Does not add a duplicate role when the user already has it")
        void isIdempotentForExistingRole() {
            UserAccount user = em.persistAndFlush(anActiveUserAccount().email("role2@example.com").build());
            em.clear();

            repository.insertRoleIfNotExists(user.getId(), "USER");
            em.clear();
            repository.insertRoleIfNotExists(user.getId(), "USER");
            em.clear();

            UserAccount reloaded = em.find(UserAccount.class, user.getId());
            assertThat(reloaded.getRoles()).containsOnly(Role.USER);
        }
    }
}
