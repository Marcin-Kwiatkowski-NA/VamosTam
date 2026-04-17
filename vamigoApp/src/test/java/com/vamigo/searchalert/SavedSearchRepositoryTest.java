package com.vamigo.searchalert;

import com.vamigo.AbstractIntegrationTest;
import com.vamigo.user.UserAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.vamigo.util.Constants.*;
import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SavedSearchRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SavedSearchRepository repository;

    private UserAccount user;
    private UserAccount creator;
    private LocalDate departureDate;

    @BeforeEach
    void setUp() {
        user = em.persistAndFlush(anActiveUserAccount().email("watcher@example.com").build());
        creator = em.persistAndFlush(anActiveUserAccount().email("creator@example.com").build());
        departureDate = LocalDate.now().plusDays(7);
    }

    @Nested
    @DisplayName("Find active saved searches for a user")
    class FindActiveByUserTests {

        @Test
        @DisplayName("Returns only searches flagged active for the given user")
        void returnsOnlyActiveSearchesForUser() {
            em.persistAndFlush(aSavedSearch(user).departureDate(departureDate).active(true).build());
            em.persistAndFlush(aSavedSearch(user).departureDate(departureDate)
                    .originOsmId(OSM_ID_KRAKOW).active(false).build());
            em.clear();

            List<SavedSearch> active = repository.findByUserIdAndActiveTrue(user.getId());

            assertThat(active).hasSize(1)
                    .first().extracting(SavedSearch::isActive).isEqualTo(true);
        }
    }

    @Nested
    @DisplayName("Count manually created active searches for a user")
    class CountManualActiveTests {

        @Test
        @DisplayName("Counts only active searches that were not auto-created")
        void countsManualActiveOnly() {
            em.persistAndFlush(aSavedSearch(user).departureDate(departureDate)
                    .active(true).autoCreated(false).build());
            em.persistAndFlush(aSavedSearch(user).departureDate(departureDate)
                    .originOsmId(OSM_ID_KRAKOW).active(true).autoCreated(true).build());
            em.persistAndFlush(aSavedSearch(user).departureDate(departureDate)
                    .originOsmId(OSM_ID_WARSAW).active(false).autoCreated(false).build());
            em.clear();

            long count = repository.countByUserIdAndActiveTrueAndAutoCreatedFalse(user.getId());

            assertThat(count).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Find a saved search by id scoped to its owner")
    class FindByIdAndUserTests {

        @Test
        @DisplayName("Returns the search when the user id matches the owner")
        void returnsSearchWhenOwnerMatches() {
            SavedSearch s = em.persistAndFlush(aSavedSearch(user).departureDate(departureDate).build());
            em.clear();

            Optional<SavedSearch> found = repository.findByIdAndUserId(s.getId(), user.getId());

            assertThat(found).isPresent();
        }

        @Test
        @DisplayName("Returns empty when the search exists but belongs to another user")
        void returnsEmptyWhenWrongUser() {
            SavedSearch s = em.persistAndFlush(aSavedSearch(user).departureDate(departureDate).build());
            em.clear();

            assertThat(repository.findByIdAndUserId(s.getId(), creator.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Check whether an active saved search already exists for the user")
    class ExistsActiveSearchTests {

        @Test
        @DisplayName("Returns true when an active search matches user, route, date and type")
        void returnsTrueWhenAllMatch() {
            em.persistAndFlush(aSavedSearch(user).departureDate(departureDate).active(true).build());
            em.clear();

            boolean exists = repository
                    .existsByUserIdAndOriginOsmIdAndDestinationOsmIdAndDepartureDateAndSearchTypeAndActiveTrue(
                            user.getId(), OSM_ID_ORIGIN, OSM_ID_DESTINATION, departureDate, SearchType.RIDE);

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Returns false when the only matching search is inactive")
        void returnsFalseWhenInactive() {
            em.persistAndFlush(aSavedSearch(user).departureDate(departureDate).active(false).build());
            em.clear();

            boolean exists = repository
                    .existsByUserIdAndOriginOsmIdAndDestinationOsmIdAndDepartureDateAndSearchTypeAndActiveTrue(
                            user.getId(), OSM_ID_ORIGIN, OSM_ID_DESTINATION, departureDate, SearchType.RIDE);

            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Find saved searches matching a new ride via PostGIS ST_DWithin")
    class FindMatchingSearchesTests {

        @Test
        @DisplayName("Flags the result as exact match when origin and destination osmIds both match")
        void returnsExactMatchWithFlagTrue() {
            em.persistAndFlush(aSavedSearch(user).departureDate(departureDate).active(true).build());
            em.clear();

            List<Object[]> results = repository.findMatchingSearches(
                    SearchType.RIDE, departureDate, creator.getId(),
                    OSM_ID_ORIGIN, OSM_ID_DESTINATION,
                    LAT_ORIGIN, LON_ORIGIN, LAT_DESTINATION, LON_DESTINATION,
                    1000.0);

            assertThat(results).hasSize(1);
            Object[] row = results.get(0);
            assertThat(row).hasSizeGreaterThan(1);
            assertThat(row[row.length - 1]).isInstanceOf(Boolean.class);
            assertThat((Boolean) row[row.length - 1]).isTrue();
        }

        @Test
        @DisplayName("Excludes searches belonging to the ride creator themselves")
        void excludesCreatorsOwnSearch() {
            em.persistAndFlush(aSavedSearch(creator).departureDate(departureDate).active(true).build());
            em.clear();

            List<Object[]> results = repository.findMatchingSearches(
                    SearchType.RIDE, departureDate, creator.getId(),
                    OSM_ID_ORIGIN, OSM_ID_DESTINATION,
                    LAT_ORIGIN, LON_ORIGIN, LAT_DESTINATION, LON_DESTINATION,
                    1000.0);

            assertThat(results).isEmpty();
        }

        @DisplayName("Matches only when ride coordinates fall within the search radius")
        @ParameterizedTest(name = "radius={0}m → expectMatch={1}")
        @CsvSource({
                "10000, true",
                "100, false"
        })
        void radiusBoundary(double radiusMeters, boolean expectMatch) {
            // Saved search is at (50.0, 20.0) origin and (52.0, 21.0) destination.
            // Probe with shifted coordinates ~5km north (≈0.045°) so a 10km radius matches but 100m does not.
            em.persistAndFlush(aSavedSearch(user).departureDate(departureDate).active(true).build());
            em.clear();

            double shiftedOriginLat = LAT_ORIGIN + 0.045;
            double shiftedDestLat = LAT_DESTINATION + 0.045;
            // OSM IDs intentionally mismatched (9_999_998L / 9_999_999L) so the exact_match CASE
            // evaluates false; only the proximity (ST_DWithin) branch can match.
            List<Object[]> results = repository.findMatchingSearches(
                    SearchType.RIDE, departureDate, creator.getId(),
                    9_999_998L, 9_999_999L,
                    shiftedOriginLat, LON_ORIGIN, shiftedDestLat, LON_DESTINATION,
                    radiusMeters);

            if (expectMatch) {
                assertThat(results).hasSize(1);
                Object[] row = results.get(0);
                assertThat(row).hasSizeGreaterThan(1);
                assertThat(row[row.length - 1]).isInstanceOf(Boolean.class);
                assertThat((Boolean) row[row.length - 1]).isFalse();
            } else {
                assertThat(results).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("Delete auto-created saved searches for a user by route, date and type")
    class DeleteAutoCreatedTests {

        @Test
        @DisplayName("Deletes only auto-created matches and keeps manually created ones")
        void deletesOnlyAutoCreatedMatching() {
            SavedSearch auto = em.persistAndFlush(aSavedSearch(user).departureDate(departureDate)
                    .autoCreated(true).build());
            SavedSearch manual = em.persistAndFlush(aSavedSearch(user).departureDate(departureDate)
                    .originOsmId(OSM_ID_KRAKOW).autoCreated(false).build());
            em.clear();

            repository
                    .deleteByUserIdAndOriginOsmIdAndDestinationOsmIdAndDepartureDateAndSearchTypeAndAutoCreatedTrue(
                            user.getId(), OSM_ID_ORIGIN, OSM_ID_DESTINATION, departureDate, SearchType.RIDE);
            em.flush();
            em.clear();

            assertThat(em.find(SavedSearch.class, auto.getId())).isNull();
            assertThat(em.find(SavedSearch.class, manual.getId())).isNotNull();
        }
    }
}
