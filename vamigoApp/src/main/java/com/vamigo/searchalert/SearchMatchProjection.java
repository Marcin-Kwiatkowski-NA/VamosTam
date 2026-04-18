package com.vamigo.searchalert;

/**
 * Projection returned by {@link SavedSearchRepository#findMatchingSearches} —
 * carries only the saved-search id, per-side distances, and the exact-match
 * flag that {@link SearchAlertMatcher} needs.
 */
public interface SearchMatchProjection {

    Long getSavedSearchId();

    Integer getOriginDistanceM();

    Integer getDestinationDistanceM();

    Boolean getExactMatch();
}
