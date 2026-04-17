package com.vamigo.location;

import com.vamigo.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static com.vamigo.util.Constants.*;
import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class LocationRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private LocationRepository repository;

    @Nested
    @DisplayName("Lookup location by OSM id")
    class FindByOsmIdTests {

        @Test
        @DisplayName("Returns location when OSM id matches a persisted row")
        void returnsLocationWhenOsmIdMatches() {
            Location persisted = em.persistAndFlush(anOriginLocation().id(null).build());
            em.clear();

            Optional<Location> found = repository.findByOsmId(OSM_ID_ORIGIN);

            assertThat(found).isPresent()
                    .get().extracting(Location::getId).isEqualTo(persisted.getId());
        }

        @Test
        @DisplayName("Returns empty when no location has the given OSM id")
        void returnsEmptyWhenOsmIdMissing() {
            em.persistAndFlush(anOriginLocation().id(null).build());
            em.clear();

            Optional<Location> found = repository.findByOsmId(999_999L);

            assertThat(found).isEmpty();
        }
    }
}
