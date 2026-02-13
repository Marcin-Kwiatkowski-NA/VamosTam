package com.blablatwo.ride;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RideBookingRepository extends JpaRepository<RideBooking, Long> {

    Optional<RideBooking> findByRideIdAndPassengerId(Long rideId, Long passengerId);

    boolean existsByRideIdAndPassengerId(Long rideId, Long passengerId);

    List<RideBooking> findByPassengerId(Long passengerId);
}
