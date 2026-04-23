package com.vamigo.ride;

import com.jayway.jsonpath.JsonPath;
import com.vamigo.AbstractFullStackTest;
import com.vamigo.location.Location;
import com.vamigo.user.UserAccount;
import com.vamigo.util.IntegrationFixtures;
import com.vamigo.util.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.vamigo.util.Constants.OSM_ID_DESTINATION;
import static com.vamigo.util.Constants.OSM_ID_ORIGIN;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end check that {@code GET /rides/search} paginates stably when several
 * rides share the same {@code departureTime}. All four rides must appear exactly
 * once across consecutive pages, ordered by ascending id.
 */
class RidesControllerSearchStableSortIT extends AbstractFullStackTest {

    @Autowired
    IntegrationFixtures fx;

    @Test
    void searchRides_paginatesStably_onDepartureTimeTies() {
        UserAccount driver = fx.persistUser();
        Location origin = fx.persistLocation(TestFixtures.anOriginLocation());
        Location destination = fx.persistLocation(TestFixtures.aDestinationLocation());

        List<Long> createdIds = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            createdIds.add(fx.persistRideWithStops(driver, 2, true, origin, destination).getId());
        }
        createdIds.sort(Comparator.naturalOrder());

        List<Long> combined = new ArrayList<>();
        combined.addAll(idsOnPage(0, 2));
        combined.addAll(idsOnPage(1, 2));

        assertThat(combined)
                .as("every ride appears exactly once across both pages, ordered by id")
                .containsExactlyElementsOf(createdIds);
    }

    private List<Long> idsOnPage(int page, int size) {
        MvcTestResult result = mvc.get()
                .uri("/rides/search?originOsmId={o}&destinationOsmId={d}&minAvailableSeats=1&page={p}&size={s}",
                        OSM_ID_ORIGIN, OSM_ID_DESTINATION, page, size)
                .exchange();

        assertThat(result).hasStatusOk();

        try {
            List<Number> rawIds = JsonPath.read(
                    result.getResponse().getContentAsString(), "$.content[*].id");
            return rawIds.stream().map(Number::longValue).toList();
        } catch (java.io.UnsupportedEncodingException e) {
            throw new AssertionError("Unable to decode search response body", e);
        }
    }
}
