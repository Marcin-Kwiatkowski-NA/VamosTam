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

import java.time.Instant;
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

        // Deduplicate by savedSearch ID, keeping exactMatch = true if ANY pair was exact
        Map<Long, Boolean> matchedSearches = new HashMap<>();

        for (int i = 0; i < stops.size(); i++) {
            for (int j = i + 1; j < stops.size(); j++) {
                LocationDto origin = stops.get(i).location();
                LocationDto dest = stops.get(j).location();

                List<Object[]> results = savedSearchRepository.findMatchingSearches(
                        SearchType.RIDE, departureDate, driverId,
                        origin.osmId(), dest.osmId(),
                        origin.latitude(), origin.longitude(),
                        dest.latitude(), dest.longitude(),
                        radiusMeters);

                for (Object[] row : results) {
                    Long ssId = ((Number) row[0]).longValue();
                    boolean exactMatch = (Boolean) row[row.length - 1];
                    matchedSearches.merge(ssId, exactMatch, (existing, incoming) -> existing || incoming);
                }
            }
        }

        writeOutboxRows(matchedSearches, ride.id(), null);
        log.debug("Ride {} matched {} saved searches", ride.id(), matchedSearches.size());
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

        List<Object[]> results = savedSearchRepository.findMatchingSearches(
                SearchType.SEAT, departureDate, userId,
                origin.osmId(), dest.osmId(),
                origin.latitude(), origin.longitude(),
                dest.latitude(), dest.longitude(),
                radiusMeters);

        Map<Long, Boolean> matchedSearches = new HashMap<>();
        for (Object[] row : results) {
            Long ssId = ((Number) row[0]).longValue();
            boolean exactMatch = (Boolean) row[row.length - 1];
            matchedSearches.merge(ssId, exactMatch, (existing, incoming) -> existing || incoming);
        }

        writeOutboxRows(matchedSearches, null, seat.id());
        log.debug("Seat {} matched {} saved searches", seat.id(), matchedSearches.size());
    }

    private void writeOutboxRows(Map<Long, Boolean> matchedSearches, Long rideId, Long seatId) {
        for (var entry : matchedSearches.entrySet()) {
            SavedSearch ss = savedSearchRepository.getReferenceById(entry.getKey());
            matchRepository.save(SearchAlertMatch.builder()
                    .savedSearch(ss)
                    .rideId(rideId)
                    .seatId(seatId)
                    .exactMatch(entry.getValue())
                    .build());
        }
    }
}
