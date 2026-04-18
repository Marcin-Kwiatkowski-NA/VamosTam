package com.vamigo.match;

/**
 * Shared score for a matched offer. Distances are in meters;
 * {@link #combinedM()} is {@code originDistanceM + destinationDistanceM}.
 * {@code null} distances mean that side matched by exact OSM id
 * (no proximity offset — treated as zero when combining).
 */
public record MatchScore(
        Integer originDistanceM,
        Integer destinationDistanceM,
        boolean exactMatch
) {

    public int combinedM() {
        return (originDistanceM != null ? originDistanceM : 0)
                + (destinationDistanceM != null ? destinationDistanceM : 0);
    }

    public static MatchScore exact() {
        return new MatchScore(null, null, true);
    }

    public static MatchScore proximity(int originM, int destM) {
        return new MatchScore(originM, destM, false);
    }
}
