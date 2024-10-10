package com.blablatwo.ride;

import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;

import java.util.Optional;

public interface RideService {
    Optional<RideResponseDto> getById(long id);
    RideResponseDto create(RideCreationDto ride);

    RideResponseDto update(RideCreationDto ride, long id, String ifMatch);

    void delete(long id);
}
