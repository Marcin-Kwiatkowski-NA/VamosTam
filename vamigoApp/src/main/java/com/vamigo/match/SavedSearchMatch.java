package com.vamigo.match;

/**
 * A saved-search hit for a newly created ride or seat. The "winning"
 * stop-pair's origin/destination are captured so outbox rows can display
 * the offset labels. Exact-match entries carry {@code null} distances
 * and names so downstream renders the saved-search city name unadorned.
 */
public record SavedSearchMatch(
        Long savedSearchId,
        boolean exactMatch,
        Integer originDistanceM,
        Integer destinationDistanceM,
        String originStopName,
        String destinationStopName
) {

    public int combinedDistanceM() {
        return (originDistanceM != null ? originDistanceM : 0)
                + (destinationDistanceM != null ? destinationDistanceM : 0);
    }
}
