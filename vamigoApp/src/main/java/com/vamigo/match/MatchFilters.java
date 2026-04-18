package com.vamigo.match;

/**
 * Filters applied alongside the geo / time predicates.
 *
 * @param driverIdFilter    if non-null, forward ride searches return only rides
 *                          whose driver id equals this value (used by "my rides"
 *                          and driver-scoped feeds).
 * @param excludedUserId    if non-null, inverse saved-search matching excludes
 *                          searches created by this user.
 * @param minAvailableSeats if non-null, ride results must have at least this
 *                          many seats (applied as a coarse {@code totalSeats}
 *                          predicate; per-segment availability is the caller's
 *                          responsibility).
 */
public record MatchFilters(
        Long driverIdFilter,
        Long excludedUserId,
        Integer minAvailableSeats
) {

    public static MatchFilters none() {
        return new MatchFilters(null, null, null);
    }

    public static MatchFilters forDriver(Long driverId) {
        return new MatchFilters(driverId, null, null);
    }

    public static MatchFilters excludeUser(Long userId) {
        return new MatchFilters(null, userId, null);
    }
}
