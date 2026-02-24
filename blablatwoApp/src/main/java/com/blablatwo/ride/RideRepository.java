package com.blablatwo.ride;

import com.blablatwo.domain.Status;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RideRepository extends JpaRepository<Ride, Long>, JpaSpecificationExecutor<Ride> {

    List<Ride> findByDriverId(Long driverId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Ride r WHERE r.id = :id")
    Optional<Ride> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        SELECT r FROM Ride r
        WHERE r.status = com.blablatwo.domain.Status.ACTIVE
        AND r.estimatedArrivalAt IS NOT NULL
        AND r.estimatedArrivalAt < :cutoff
        AND EXISTS (
            SELECT 1 FROM RideBooking b
            WHERE b.ride = r AND b.status = com.blablatwo.ride.BookingStatus.CONFIRMED
        )
        """)
    List<Ride> findActiveRidesReadyForCompletion(@Param("cutoff") Instant cutoff);

    List<Ride> findByStatusAndCompletedAtBetween(Status status, Instant from, Instant to);
}
