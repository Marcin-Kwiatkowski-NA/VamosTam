package com.blablatwo.ride.external;

import com.blablatwo.ride.dto.ExternalRideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;

import java.util.Optional;

public interface ExternalRideService {

    /**
     * Creates a ride from an external source (e.g., Facebook scraper).
     * Uses the Facebook proxy user as the driver.
     *
     * @param dto the external ride data
     * @return the created ride response
     * @throws com.blablatwo.exceptions.NoSuchCityException if origin or destination city not found
     * @throws com.blablatwo.exceptions.DuplicateExternalEntityException if externalId or content already exists
     */
    RideResponseDto createExternalRide(ExternalRideCreationDto dto);

    /**
     * Checks if an external ride already exists by external ID.
     */
    boolean existsByExternalId(String externalId);

    /**
     * Gets an external ride by its external ID.
     */
    Optional<RideResponseDto> getByExternalId(String externalId);
}
