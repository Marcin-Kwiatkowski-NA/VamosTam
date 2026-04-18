package com.vamigo.searchalert;

import com.vamigo.location.LocationDto;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.ride.dto.RideStopDto;
import com.vamigo.seat.dto.SeatResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@EnableConfigurationProperties(SearchAlertProperties.class)
public class SearchAlertMatcher {

    private static final Logger log = LoggerFactory.getLogger(SearchAlertMatcher.class);

    private final SavedSearchRepository savedSearchRepository;
    private final SearchAlertMatchRepository matchRepository;
    private final SearchAlertProperties properties;

    public SearchAlertMatcher(SavedSearchRepository savedSearchRepository,
                              SearchAlertMatchRepository matchRepository,
                              SearchAlertProperties properties) {
        this.savedSearchRepository = savedSearchRepository;
        this.matchRepository = matchRepository;
        this.properties = properties;
    }

    /**
     * Winning stop-pair per saved search across all (i, j) combinations of a ride.
     * Distance / name fields are {@code null} when that side matched exactly —
     * downstream renders the saved-search city name with no offset suffix.
     */
    private record MatchCandidate(
            boolean exactMatch,
            Integer originDistanceM,
            Integer destinationDistanceM,
            String originStopName,
            String destinationStopName
    ) {
        int totalDistance() {
            return (originDistanceM != null ? originDistanceM : 0)
                    + (destinationDistanceM != null ? destinationDistanceM : 0);
        }
    }

    /**
     * Match a newly created ride against all active saved searches looking for rides.
     * Iterates over all valid stop pairs (i, j) where i < j.
     */
    @Transactional
    public void matchRide(RideResponseDto ride, Long driverId) {
        List<RideStopDto> stops = ride.stops();
        if (stops == null || stops.size() < 2) {
            log.debug("Ride {} has fewer than 2 stops, skipping match", ride.id());
            return;
        }

        LocalDate departureDate = ride.departureTime().atOffset(ZoneOffset.UTC).toLocalDate();
        double radiusMeters = properties.proximityRadiusMeters();
        Map<Long, MatchCandidate> bestPerSearch = new HashMap<>();

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

                processResults(results, bestPerSearch, origin.name(), dest.name());
            }
        }

        writeOutboxRows(bestPerSearch, ride.id(), null);
        log.debug("Ride {} matched {} saved searches", ride.id(), bestPerSearch.size());
    }

    /**
     * Match a newly created seat against all active saved searches looking for seats.
     */
    @Transactional
    public void matchSeat(SeatResponseDto seat, Long userId) {
        LocationDto origin = seat.origin();
        LocationDto dest = seat.destination();
        if (origin == null || dest == null) return;

        LocalDate departureDate = seat.departureTime().atOffset(ZoneOffset.UTC).toLocalDate();
        double radiusMeters = properties.proximityRadiusMeters();
        Map<Long, MatchCandidate> bestPerSearch = new HashMap<>();

        List<SearchMatchProjection> results = savedSearchRepository.findMatchingSearches(
                SearchType.SEAT, departureDate, userId,
                origin.osmId(), dest.osmId(),
                origin.latitude(), origin.longitude(),
                dest.latitude(), dest.longitude(),
                radiusMeters);

        processResults(results, bestPerSearch, origin.name(), dest.name());

        writeOutboxRows(bestPerSearch, null, seat.id());
        log.debug("Seat {} matched {} saved searches", seat.id(), bestPerSearch.size());
    }

    private void processResults(List<SearchMatchProjection> results,
                                Map<Long, MatchCandidate> bestPerSearch,
                                String originName, String destName) {
        for (SearchMatchProjection row : results) {
            MatchCandidate candidate = buildCandidate(
                    Boolean.TRUE.equals(row.getExactMatch()),
                    row.getOriginDistanceM(), row.getDestinationDistanceM(),
                    originName, destName);
            bestPerSearch.merge(row.getSavedSearchId(), candidate, SearchAlertMatcher::pickBetter);
        }
    }

    private static MatchCandidate buildCandidate(boolean exactMatch,
                                                 Integer origDist, Integer destDist,
                                                 String originName, String destName) {
        int o = origDist != null ? origDist : 0;
        int d = destDist != null ? destDist : 0;
        if (exactMatch && o == 0 && d == 0) {
            return new MatchCandidate(true, null, null, null, null);
        }
        return new MatchCandidate(exactMatch, o, d, originName, destName);
    }

    private static MatchCandidate pickBetter(MatchCandidate existing, MatchCandidate incoming) {
        if (existing.exactMatch() && !incoming.exactMatch()) return existing;
        if (!existing.exactMatch() && incoming.exactMatch()) return incoming;
        return incoming.totalDistance() < existing.totalDistance() ? incoming : existing;
    }

    private void writeOutboxRows(Map<Long, MatchCandidate> matches, Long rideId, Long seatId) {
        if (matches.isEmpty()) return;

        List<SearchAlertMatch> entities = matches.entrySet().stream()
                .map(entry -> SearchAlertMatch.builder()
                        .savedSearch(savedSearchRepository.getReferenceById(entry.getKey()))
                        .rideId(rideId)
                        .seatId(seatId)
                        .exactMatch(entry.getValue().exactMatch())
                        .originStopName(entry.getValue().originStopName())
                        .originDistanceM(entry.getValue().originDistanceM())
                        .destinationStopName(entry.getValue().destinationStopName())
                        .destinationDistanceM(entry.getValue().destinationDistanceM())
                        .build())
                .toList();

        matchRepository.saveAll(entities);
    }
}
