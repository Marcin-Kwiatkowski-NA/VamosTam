package com.vamigo.match.internal;

import com.vamigo.location.LocationDto;
import com.vamigo.match.SavedSearchMatch;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.ride.dto.RideStopDto;
import com.vamigo.searchalert.SavedSearchRepository;
import com.vamigo.searchalert.SearchMatchProjection;
import com.vamigo.searchalert.SearchType;
import com.vamigo.seat.dto.SeatResponseDto;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Facade over the native saved-search matching SQL. Centralises:
 * <ul>
 *   <li>the stop-pair iteration for rides (was previously inside
 *       {@code SearchAlertMatcher});</li>
 *   <li>the "pick best candidate per saved search" merge rule.</li>
 * </ul>
 * Collapsing the stop-pair loop into a single SQL statement is an
 * acknowledged follow-up — kept here so there is only one caller of
 * {@link SavedSearchRepository#findMatchingSearches}.
 */
@Component
public class SavedSearchMatchingQuery {

    private final SavedSearchRepository savedSearchRepository;

    public SavedSearchMatchingQuery(SavedSearchRepository savedSearchRepository) {
        this.savedSearchRepository = savedSearchRepository;
    }

    public List<SavedSearchMatch> matchRide(RideResponseDto ride, Long driverId, double radiusMeters) {
        List<RideStopDto> stops = ride.stops();
        if (stops == null || stops.size() < 2) return List.of();

        LocalDate departureDate = ride.departureTime().atOffset(ZoneOffset.UTC).toLocalDate();
        Map<Long, SavedSearchMatch> bestPerSearch = new HashMap<>();

        for (int i = 0; i < stops.size(); i++) {
            for (int j = i + 1; j < stops.size(); j++) {
                LocationDto origin = stops.get(i).location();
                LocationDto dest = stops.get(j).location();

                List<SearchMatchProjection> results = savedSearchRepository.findMatchingSearches(
                        SearchType.RIDE, departureDate, driverId,
                        origin.osmId(), dest.osmId(),
                        origin.latitude(), origin.longitude(),
                        dest.latitude(), dest.longitude(),
                        radiusMeters);

                mergeResults(results, bestPerSearch, origin.name(), dest.name());
            }
        }

        return List.copyOf(bestPerSearch.values());
    }

    public List<SavedSearchMatch> matchSeat(SeatResponseDto seat, Long userId, double radiusMeters) {
        LocationDto origin = seat.origin();
        LocationDto dest = seat.destination();
        if (origin == null || dest == null) return List.of();

        LocalDate departureDate = seat.departureTime().atOffset(ZoneOffset.UTC).toLocalDate();
        Map<Long, SavedSearchMatch> bestPerSearch = new HashMap<>();

        List<SearchMatchProjection> results = savedSearchRepository.findMatchingSearches(
                SearchType.SEAT, departureDate, userId,
                origin.osmId(), dest.osmId(),
                origin.latitude(), origin.longitude(),
                dest.latitude(), dest.longitude(),
                radiusMeters);

        mergeResults(results, bestPerSearch, origin.name(), dest.name());
        return List.copyOf(bestPerSearch.values());
    }

    private static void mergeResults(List<SearchMatchProjection> results,
                                     Map<Long, SavedSearchMatch> bestPerSearch,
                                     String originName, String destName) {
        for (SearchMatchProjection row : results) {
            SavedSearchMatch candidate = buildCandidate(
                    Boolean.TRUE.equals(row.getExactMatch()),
                    row.getOriginDistanceM(), row.getDestinationDistanceM(),
                    originName, destName,
                    row.getSavedSearchId());
            bestPerSearch.merge(row.getSavedSearchId(), candidate, SavedSearchMatchingQuery::pickBetter);
        }
    }

    private static SavedSearchMatch buildCandidate(boolean exactMatch,
                                                   Integer origDist, Integer destDist,
                                                   String originName, String destName,
                                                   Long savedSearchId) {
        int o = origDist != null ? origDist : 0;
        int d = destDist != null ? destDist : 0;
        if (exactMatch && o == 0 && d == 0) {
            return new SavedSearchMatch(savedSearchId, true, null, null, null, null);
        }
        return new SavedSearchMatch(savedSearchId, exactMatch, o, d, originName, destName);
    }

    private static SavedSearchMatch pickBetter(SavedSearchMatch existing, SavedSearchMatch incoming) {
        if (existing.exactMatch() && !incoming.exactMatch()) return existing;
        if (!existing.exactMatch() && incoming.exactMatch()) return incoming;
        return incoming.combinedDistanceM() < existing.combinedDistanceM() ? incoming : existing;
    }
}
