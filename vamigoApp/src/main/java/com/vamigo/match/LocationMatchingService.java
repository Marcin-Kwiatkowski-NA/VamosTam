package com.vamigo.match;

import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.seat.dto.SeatResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Single entry point for both query directions — "find offers near a route"
 * (forward) and "find saved searches a newly-created offer matches" (inverse).
 * All PostGIS geometry / unit / SRID / scoring concerns live behind this
 * interface.
 */
public interface LocationMatchingService {

    // --- Forward: smart search, driver matching, home feed, digests -------

    Page<RideMatch> findRides(RideMatchQuery query, Pageable pageable);

    Page<SeatMatch> findSeats(SeatMatchQuery query, Pageable pageable);

    // --- Inverse: alert listener, external-import listener ----------------

    List<SavedSearchMatch> findSavedSearchesMatchingRide(RideResponseDto ride, Long driverId);

    List<SavedSearchMatch> findSavedSearchesMatchingSeat(SeatResponseDto seat, Long userId);
}
