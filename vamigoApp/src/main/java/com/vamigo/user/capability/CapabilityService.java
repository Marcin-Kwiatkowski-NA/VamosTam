package com.vamigo.user.capability;

/**
 * Service for checking user capabilities based on account status and verification.
 */
public interface CapabilityService {

    /**
     * Checks if a user can book rides.
     * Requirements: ACTIVE status + phone verified
     */
    boolean canBook(Long userId);

    /**
     * Checks if a user can create rides.
     * Requirements: ACTIVE status + verified email
     */
    boolean canCreateRide(Long userId);

    /**
     * Checks if a user can create seat offers.
     * Requirements: ACTIVE status + verified email
     */
    boolean canCreateSeat(Long userId);

    /**
     * Checks if a user account is active.
     * General-purpose check used for booking and other non-specific capabilities.
     */
    boolean isActive(Long userId);
}
