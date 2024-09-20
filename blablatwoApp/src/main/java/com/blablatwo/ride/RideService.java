package com.blablatwo.ride;

import com.blablatwo.ride.DTO.RideResponseDto;

import java.util.Optional;

public interface RideService {
    Optional<RideResponseDto> getById(long id);
    RideResponseDto create(RideCreationDTO ride);

    RideResponseDto update(RideCreationDTO ride, long id);
    boolean ifMatch(long id, String ifMatch);

    void delete(long id);
}
