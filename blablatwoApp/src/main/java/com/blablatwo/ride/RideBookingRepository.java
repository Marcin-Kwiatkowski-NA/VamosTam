package com.blablatwo.ride;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("""
        SELECT b FROM RideBooking b
        JOIN FETCH b.ride r
        JOIN FETCH r.driver
        JOIN FETCH b.passenger
        WHERE b.status = com.blablatwo.ride.BookingStatus.CONFIRMED
        AND r.status = com.blablatwo.domain.Status.COMPLETED
        AND r.completedAt > :cutoff
        AND (r.driver.id = :userId OR b.passenger.id = :userId)
        """)
    List<RideBooking> findConfirmedBookingsOnCompletedRides(
            @Param("userId") Long userId,
            @Param("cutoff") Instant cutoff);
}
