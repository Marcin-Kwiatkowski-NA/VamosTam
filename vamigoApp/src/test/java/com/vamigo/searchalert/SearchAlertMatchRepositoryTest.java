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

import java.util.List;

import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SearchAlertMatchRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SearchAlertMatchRepository repository;

    private SavedSearch search;

    @BeforeEach
    void setUp() {
        UserAccount user = em.persistAndFlush(anActiveUserAccount().build());
        search = em.persistAndFlush(aSavedSearch(user).build());
    }

    private SearchAlertMatch.SearchAlertMatchBuilder match() {
        return SearchAlertMatch.builder().savedSearch(search).rideId(1L);
    }

    @Nested
    @DisplayName("Find unsent push matches")
    class FindUnsentPushTests {

        @Test
        @DisplayName("Returns only matches whose push notification has not been sent")
        void returnsOnlyUnsentPushMatches() {
            em.persistAndFlush(match().rideId(10L).pushSent(false).build());
            em.persistAndFlush(match().rideId(11L).pushSent(true).build());
            em.clear();

            List<SearchAlertMatch> result = repository.findUnsentPush();

            assertThat(result).hasSize(1)
                    .first().extracting(SearchAlertMatch::isPushSent).isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("Find unsent email matches")
    class FindUnsentEmailTests {

        @Test
        @DisplayName("Returns only unsent matches that are flagged as exact")
        void returnsOnlyUnsentExactMatches() {
            em.persistAndFlush(match().rideId(10L).emailSent(false).exactMatch(true).build());
            em.persistAndFlush(match().rideId(11L).emailSent(false).exactMatch(false).build());
            em.persistAndFlush(match().rideId(12L).emailSent(true).exactMatch(true).build());
            em.clear();

            List<SearchAlertMatch> result = repository.findUnsentEmail();

            assertThat(result).hasSize(1)
                    .first().satisfies(m -> {
                        assertThat(m.isExactMatch()).isTrue();
                        assertThat(m.isEmailSent()).isFalse();
                    });
        }
    }

    @Nested
    @DisplayName("Delete matches by id list")
    class DeleteByIdsTests {

        @Test
        @DisplayName("Deletes only the matches whose ids are supplied and leaves the rest intact")
        void deletesGivenIds() {
            SearchAlertMatch a = em.persistAndFlush(match().rideId(10L).build());
            SearchAlertMatch b = em.persistAndFlush(match().rideId(11L).build());
            SearchAlertMatch keep = em.persistAndFlush(match().rideId(12L).build());
            em.clear();

            repository.deleteByIds(List.of(a.getId(), b.getId()));
            em.flush();
            em.clear();

            assertThat(em.find(SearchAlertMatch.class, a.getId())).isNull();
            assertThat(em.find(SearchAlertMatch.class, b.getId())).isNull();
            assertThat(em.find(SearchAlertMatch.class, keep.getId())).isNotNull();
        }
    }

    @Nested
    @DisplayName("Route offset columns")
    class RouteOffsetColumns {

        @Test
        @DisplayName("Round-trips nearby-match stop names and distances on both sides")
        void roundTripsNearbyFields() {
            SearchAlertMatch persisted = em.persistAndFlush(match()
                    .rideId(20L)
                    .exactMatch(false)
                    .originStopName("Wieliczka")
                    .originDistanceM(5400)
                    .destinationStopName("Pr\u00f3szk\u00f3w")
                    .destinationDistanceM(14200)
                    .build());
            em.clear();

            SearchAlertMatch reloaded = em.find(SearchAlertMatch.class, persisted.getId());
            assertThat(reloaded.getOriginStopName()).isEqualTo("Wieliczka");
            assertThat(reloaded.getOriginDistanceM()).isEqualTo(5400);
            assertThat(reloaded.getDestinationStopName()).isEqualTo("Pr\u00f3szk\u00f3w");
            assertThat(reloaded.getDestinationDistanceM()).isEqualTo(14200);
            assertThat(reloaded.isExactMatch()).isFalse();
        }

        @Test
        @DisplayName("Leaves offset columns null for fully exact matches")
        void nullForExactMatch() {
            SearchAlertMatch persisted = em.persistAndFlush(match()
                    .rideId(21L)
                    .exactMatch(true)
                    .build());
            em.clear();

            SearchAlertMatch reloaded = em.find(SearchAlertMatch.class, persisted.getId());
            assertThat(reloaded.getOriginStopName()).isNull();
            assertThat(reloaded.getOriginDistanceM()).isNull();
            assertThat(reloaded.getDestinationStopName()).isNull();
            assertThat(reloaded.getDestinationDistanceM()).isNull();
            assertThat(reloaded.isExactMatch()).isTrue();
        }
    }

    @Nested
    @DisplayName("Delete matches by saved search id")
    class DeleteBySavedSearchIdTests {

        @Test
        @DisplayName("Removes every match linked to the given saved search")
        void cascadesDeleteForSearch() {
            em.persistAndFlush(match().rideId(10L).build());
            em.persistAndFlush(match().rideId(11L).build());
            em.clear();

            repository.deleteBySavedSearchId(search.getId());
            em.flush();
            em.clear();

            assertThat(repository.findUnsentPush()).isEmpty();
        }
    }
}
