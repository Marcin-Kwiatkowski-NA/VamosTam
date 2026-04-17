package com.vamigo.user;

import com.vamigo.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class CarrierProfileRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CarrierProfileRepository repository;

    private UserAccount account;

    @BeforeEach
    void setUp() {
        account = em.persistAndFlush(anActiveUserAccount().build());
    }

    @Nested
    @DisplayName("Lookup carrier profile by slug")
    class FindBySlugTests {

        @Test
        @DisplayName("Returns profile when slug matches a saved carrier")
        void returnsProfileWhenSlugMatches() {
            em.persistAndFlush(aCarrierProfile(account).slug("acme").build());
            em.clear();

            assertThat(repository.findBySlug("acme")).isPresent()
                    .get().extracting(CarrierProfile::getSlug).isEqualTo("acme");
        }

        @Test
        @DisplayName("Returns empty when no carrier uses the given slug")
        void returnsEmptyWhenSlugAbsent() {
            assertThat(repository.findBySlug("missing")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Check if slug is already used")
    class ExistsBySlugTests {

        @Test
        @DisplayName("Returns true for persisted slugs and false for unused ones")
        void reflectsInsertedProfile() {
            em.persistAndFlush(aCarrierProfile(account).slug("acme").build());
            em.clear();

            assertThat(repository.existsBySlug("acme")).isTrue();
            assertThat(repository.existsBySlug("ghost")).isFalse();
        }
    }

    @Nested
    @DisplayName("Check if NIP is already used")
    class ExistsByNipTests {

        @Test
        @DisplayName("Returns true for persisted NIP values and false for unused ones")
        void reflectsInsertedProfile() {
            em.persistAndFlush(aCarrierProfile(account).nip("1234567890").build());
            em.clear();

            assertThat(repository.existsByNip("1234567890")).isTrue();
            assertThat(repository.existsByNip("9999999999")).isFalse();
        }
    }
}
