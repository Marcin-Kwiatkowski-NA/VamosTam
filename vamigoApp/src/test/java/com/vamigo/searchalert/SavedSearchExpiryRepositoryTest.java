package com.vamigo.searchalert;

import com.vamigo.AbstractIntegrationTest;
import com.vamigo.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static com.vamigo.util.Constants.OSM_ID_KRAKOW;
import static com.vamigo.util.Constants.OSM_ID_WARSAW;
import static com.vamigo.util.TestFixtures.aSavedSearch;
import static com.vamigo.util.TestFixtures.anActiveUserAccount;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SavedSearchExpiryRepositoryTest extends AbstractIntegrationTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SavedSearchRepository repository;

    private UserAccount user;

    @BeforeEach
    void setUp() {
        user = em.persistAndFlush(anActiveUserAccount().email("watcher@example.com").build());
    }

    @Test
    @DisplayName("Deactivates only active searches whose departure date is strictly before today")
    void deactivatesOnlyPastActiveSearches() {
        Clock fixed = Clock.fixed(Instant.parse("2026-04-18T12:00:00Z"), WARSAW);
        LocalDate today = LocalDate.now(fixed);

        SavedSearch pastActive = em.persistAndFlush(aSavedSearch(user)
                .departureDate(today.minusDays(1))
                .active(true).build());
        SavedSearch todayActive = em.persistAndFlush(aSavedSearch(user)
                .originOsmId(OSM_ID_KRAKOW)
                .departureDate(today)
                .active(true).build());
        SavedSearch futureActive = em.persistAndFlush(aSavedSearch(user)
                .originOsmId(OSM_ID_WARSAW)
                .departureDate(today.plusDays(1))
                .active(true).build());
        SavedSearch pastInactive = em.persistAndFlush(aSavedSearch(user)
                .originOsmId(OSM_ID_KRAKOW + 1)
                .departureDate(today.minusDays(3))
                .active(false).build());
        em.flush();
        em.clear();

        int updated = repository.deactivateExpired(today);
        em.flush();
        em.clear();

        assertThat(updated).isEqualTo(1);
        assertThat(em.find(SavedSearch.class, pastActive.getId()).isActive()).isFalse();
        assertThat(em.find(SavedSearch.class, todayActive.getId()).isActive()).isTrue();
        assertThat(em.find(SavedSearch.class, futureActive.getId()).isActive()).isTrue();
        assertThat(em.find(SavedSearch.class, pastInactive.getId()).isActive()).isFalse();
    }
}
