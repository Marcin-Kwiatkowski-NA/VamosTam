package com.vamigo.ride;

import com.vamigo.AbstractIntegrationTest;
import com.vamigo.domain.Status;
import com.vamigo.location.Location;
import com.vamigo.user.UserAccount;
import com.vamigo.vehicle.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RideExternalMetaRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RideExternalMetaRepository repository;

    private Location origin;
    private Location destination;
    private UserAccount driver;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        origin = em.persistAndFlush(anOriginLocation().id(null).build());
        destination = em.persistAndFlush(aDestinationLocation().id(null).build());
        driver = em.persistAndFlush(anActiveUserAccount().build());
        em.persistAndFlush(aUserProfile(driver).build());
        vehicle = em.persistAndFlush(aTesla().id(null).owner(driver).build());
    }

    private Ride persistRide() {
        Ride r = aRide(origin, destination)
                .id(null).driver(driver).vehicle(vehicle)
                .status(Status.ACTIVE).source(RideSource.FACEBOOK)
                .stops(new ArrayList<>()).bookings(new ArrayList<>()).build();
        r.replaceStops(buildStops(r, origin, destination));
        return em.persistAndFlush(r);
    }

    private RideExternalMeta persistMeta(String externalId) {
        Ride ride = persistRide();
        return em.persistAndFlush(aRideExternalMeta(ride).externalId(externalId).build());
    }

    @Nested
    @DisplayName("Lookup ride meta by external id")
    class FindByExternalIdTests {

        @Test
        @DisplayName("Returns meta when a ride with the external id has been imported")
        void returnsMetaWhenExternalIdMatches() {
            RideExternalMeta m = persistMeta("fb-42");
            em.clear();

            Optional<RideExternalMeta> found = repository.findByExternalId("fb-42");

            assertThat(found).isPresent()
                    .get().extracting(RideExternalMeta::getId).isEqualTo(m.getId());
        }

        @Test
        @DisplayName("Returns empty when no ride has the given external id")
        void returnsEmptyWhenAbsent() {
            assertThat(repository.findByExternalId("nope")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Check if external id has already been imported")
    class ExistsByExternalIdTests {

        @Test
        @DisplayName("Returns true for imported external ids and false for unknown ones")
        void reflectsInsertedRows() {
            persistMeta("fb-1");
            em.clear();

            assertThat(repository.existsByExternalId("fb-1")).isTrue();
            assertThat(repository.existsByExternalId("fb-2")).isFalse();
        }
    }

    @Nested
    @DisplayName("Load multiple ride metas by id")
    class FindAllByIdInTests {

        @Test
        @DisplayName("Returns only metas whose ids are in the requested set")
        void returnsMetasWithMatchingIds() {
            RideExternalMeta a = persistMeta("fb-a");
            RideExternalMeta b = persistMeta("fb-b");
            persistMeta("fb-other");
            em.clear();

            List<RideExternalMeta> found = repository.findAllByIdIn(List.of(a.getId(), b.getId()));

            assertThat(found).extracting(RideExternalMeta::getExternalId)
                    .containsExactlyInAnyOrder("fb-a", "fb-b");
        }
    }

    @Nested
    @DisplayName("Bulk-delete ride metas by id")
    class DeleteAllByIdInTests {

        @Test
        @DisplayName("Deletes only metas whose ids are in the requested set")
        void deletesMetasWithMatchingIds() {
            RideExternalMeta drop = persistMeta("fb-drop");
            RideExternalMeta keep = persistMeta("fb-keep");
            em.clear();

            repository.deleteAllByIdIn(List.of(drop.getId()));
            em.flush();
            em.clear();

            assertThat(em.find(RideExternalMeta.class, drop.getId())).isNull();
            assertThat(em.find(RideExternalMeta.class, keep.getId())).isNotNull();
        }
    }
}
