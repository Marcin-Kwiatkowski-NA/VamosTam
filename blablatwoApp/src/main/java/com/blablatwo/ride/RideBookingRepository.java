package com.blablatwo.ride;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Repository
public interface RideBookingRepository extends JpaRepository<RideBooking, Long> {

    boolean existsByRideIdAndPassengerIdAndStatusIn(Long rideId, Long passengerId,
                                                     Collection<BookingStatus> statuses);

    List<RideBooking> findByRideId(Long rideId);

    List<RideBooking> findByRideIdAndStatusIn(Long rideId, Collection<BookingStatus> statuses);

    List<RideBooking> findByPassengerId(Long passengerId);

    List<RideBooking> findByPassengerIdAndStatusIn(Long passengerId, Collection<BookingStatus> statuses);

    List<RideBooking> findByStatusAndBookedAtBefore(BookingStatus status, Instant cutoff);

    void deleteByPassengerId(Long passengerId);
}
