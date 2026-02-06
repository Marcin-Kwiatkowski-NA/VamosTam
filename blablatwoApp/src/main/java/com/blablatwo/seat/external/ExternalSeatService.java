package com.blablatwo.seat.external;

import com.blablatwo.seat.dto.ExternalSeatCreationDto;
import com.blablatwo.seat.dto.SeatResponseDto;

import java.util.Optional;

public interface ExternalSeatService {

    /**
     * Creates a seat from an external source (e.g., Facebook scraper).
     * Uses the Facebook proxy user as the passenger.
     *
     * @param dto the external seat data
     * @return the created seat response
     * @throws com.blablatwo.exceptions.NoSuchCityException if origin or destination city not found
     * @throws com.blablatwo.exceptions.DuplicateExternalEntityException if externalId or content already exists
     */
    SeatResponseDto createExternalSeat(ExternalSeatCreationDto dto);

    boolean existsByExternalId(String externalId);

    Optional<SeatResponseDto> getByExternalId(String externalId);
}
