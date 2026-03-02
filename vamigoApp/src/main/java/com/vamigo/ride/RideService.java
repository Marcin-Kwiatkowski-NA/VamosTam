package com.vamigo.ride;

import com.vamigo.ride.dto.RideCreationDto;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.ride.dto.RideSearchCriteriaDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RideService {
    Optional<RideResponseDto> getById(Long id);

    RideResponseDto createForCurrentUser(RideCreationDto ride, Long userId);

    RideResponseDto update(RideCreationDto ride, Long id, Long driverId);

    void delete(Long id);

    void cancelRide(Long id, Long driverId);

    RideResponseDto completeRide(Long rideId, Long driverId);

    Page<RideResponseDto> searchRides(RideSearchCriteriaDto criteria, Pageable pageable);

    Page<RideResponseDto> getAllRides(Pageable pageable);

    List<RideResponseDto> getRidesForDriver(Long driverId);
}
