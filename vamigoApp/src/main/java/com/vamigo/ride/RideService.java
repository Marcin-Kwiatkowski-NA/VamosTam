package com.vamigo.ride;

import com.vamigo.ride.dto.RideCreationDto;
import com.vamigo.ride.dto.RideListDto;
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

    void delete(Long id, Long driverId);

    void cancelRide(Long id, Long driverId);

    RideResponseDto completeRide(Long rideId, Long driverId);

    Page<RideListDto> searchRides(RideSearchCriteriaDto criteria, Pageable pageable);

    Page<RideListDto> getAllRides(Pageable pageable);

    List<RideListDto> getRidesForDriver(Long driverId);
}
