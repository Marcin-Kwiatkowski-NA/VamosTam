package com.blablatwo.ride;

import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.ride.dto.RideSearchCriteriaDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RideService {
    Optional<RideResponseDto> getById(Long id);

    RideResponseDto create(RideCreationDto ride);

    RideResponseDto update(RideCreationDto ride, Long id);

    void delete(Long id);

    Page<RideResponseDto> searchRides(RideSearchCriteriaDto criteria, Pageable pageable);

    Page<RideResponseDto> getAllRides(Pageable pageable);

    RideResponseDto bookRide(Long rideId, Long passengerId);

    RideResponseDto cancelBooking(Long rideId, Long passengerId);

    List<RideResponseDto> getRidesForPassenger(Long passengerId);
}