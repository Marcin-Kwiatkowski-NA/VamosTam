package com.vamigo.searchalert;

import com.vamigo.match.LocationMatchingService;
import com.vamigo.match.SavedSearchMatch;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.seat.dto.SeatResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Writes outbox rows for saved-search matches. All geo / stop-pair /
 * scoring concerns live inside {@link LocationMatchingService} — this
 * class only translates {@link SavedSearchMatch} records into
 * {@link SearchAlertMatch} entities.
 */
@Component
public class SearchAlertMatcher {

    private static final Logger log = LoggerFactory.getLogger(SearchAlertMatcher.class);

    private final LocationMatchingService locationMatchingService;
    private final SavedSearchRepository savedSearchRepository;
    private final SearchAlertMatchRepository matchRepository;

    public SearchAlertMatcher(LocationMatchingService locationMatchingService,
                              SavedSearchRepository savedSearchRepository,
                              SearchAlertMatchRepository matchRepository) {
        this.locationMatchingService = locationMatchingService;
        this.savedSearchRepository = savedSearchRepository;
        this.matchRepository = matchRepository;
    }

    @Transactional
    public void matchRide(RideResponseDto ride, Long driverId) {
        List<SavedSearchMatch> matches = locationMatchingService
                .findSavedSearchesMatchingRide(ride, driverId);
        writeOutboxRows(matches, ride.id(), null);
        log.debug("Ride {} matched {} saved searches", ride.id(), matches.size());
    }

    @Transactional
    public void matchSeat(SeatResponseDto seat, Long userId) {
        List<SavedSearchMatch> matches = locationMatchingService
                .findSavedSearchesMatchingSeat(seat, userId);
        writeOutboxRows(matches, null, seat.id());
        log.debug("Seat {} matched {} saved searches", seat.id(), matches.size());
    }

    private void writeOutboxRows(List<SavedSearchMatch> matches, Long rideId, Long seatId) {
        if (matches.isEmpty()) return;

        List<SearchAlertMatch> entities = matches.stream()
                .map(m -> SearchAlertMatch.builder()
                        .savedSearch(savedSearchRepository.getReferenceById(m.savedSearchId()))
                        .rideId(rideId)
                        .seatId(seatId)
                        .exactMatch(m.exactMatch())
                        .originStopName(m.originStopName())
                        .originDistanceM(m.originDistanceM())
                        .destinationStopName(m.destinationStopName())
                        .destinationDistanceM(m.destinationDistanceM())
                        .build())
                .toList();

        matchRepository.saveAll(entities);
    }
}
