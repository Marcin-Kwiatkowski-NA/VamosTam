package com.vamigo.seat;

import com.vamigo.AbstractIntegrationTest;
import com.vamigo.location.Location;
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
import java.util.Optional;

import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class SeatExternalMetaRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SeatExternalMetaRepository repository;

    private Location origin;
    private Location destination;
    private UserAccount passenger;

    @BeforeEach
    void setUp() {
        origin = em.persistAndFlush(anOriginLocation().id(null).build());
        destination = em.persistAndFlush(aDestinationLocation().id(null).build());
        passenger = em.persistAndFlush(anActiveUserAccount().build());
    }

    private SeatExternalMeta persistMetaForNewSeat(String externalId) {
        Seat seat = em.persistAndFlush(aSeat(passenger, origin, destination).build());
        return em.persistAndFlush(aSeatExternalMeta(seat).externalId(externalId).build());
    }

    @Nested
    @DisplayName("Lookup seat meta by external id")
    class FindByExternalIdTests {

        @Test
        @DisplayName("Returns meta when a seat with the external id has been imported")
        void returnsMetaWhenExternalIdMatches() {
            SeatExternalMeta meta = persistMetaForNewSeat("ext-known");
            em.clear();

            Optional<SeatExternalMeta> found = repository.findByExternalId("ext-known");

            assertThat(found).isPresent()
                    .get().extracting(SeatExternalMeta::getId).isEqualTo(meta.getId());
        }

        @Test
        @DisplayName("Returns empty when no seat has the given external id")
        void returnsEmptyWhenAbsent() {
            assertThat(repository.findByExternalId("nope")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Check if seat external id has already been imported")
    class ExistsByExternalIdTests {

        @Test
        @DisplayName("Returns true when a seat with the external id is already persisted")
        void returnsTrueWhenPresent() {
            persistMetaForNewSeat("ext-present");
            em.clear();

            assertThat(repository.existsByExternalId("ext-present")).isTrue();
        }

        @Test
        @DisplayName("Returns false when no seat has been imported with that external id")
        void returnsFalseWhenAbsent() {
            assertThat(repository.existsByExternalId("ext-missing")).isFalse();
        }
    }

    @Nested
    @DisplayName("Load multiple seat metas by id")
    class FindAllByIdInTests {

        @Test
        @DisplayName("Returns only metas whose ids are in the requested set")
        void returnsMetasWithMatchingSeatIds() {
            SeatExternalMeta a = persistMetaForNewSeat("ext-a");
            SeatExternalMeta b = persistMetaForNewSeat("ext-b");
            persistMetaForNewSeat("ext-other");
            em.clear();

            List<SeatExternalMeta> found = repository.findAllByIdIn(List.of(a.getId(), b.getId()));

            assertThat(found).extracting(SeatExternalMeta::getExternalId)
                    .containsExactlyInAnyOrder("ext-a", "ext-b");
        }
    }

    @Nested
    @DisplayName("Bulk-delete seat metas by id")
    class DeleteAllByIdInTests {

        @Test
        @DisplayName("Deletes only metas whose ids are in the requested set")
        void deletesGivenMetas() {
            SeatExternalMeta a = persistMetaForNewSeat("ext-a");
            SeatExternalMeta keep = persistMetaForNewSeat("ext-keep");
            em.clear();

            repository.deleteAllByIdIn(List.of(a.getId()));
            em.flush();
            em.clear();

            assertThat(em.find(SeatExternalMeta.class, a.getId())).isNull();
            assertThat(em.find(SeatExternalMeta.class, keep.getId())).isNotNull();
        }
    }
}
