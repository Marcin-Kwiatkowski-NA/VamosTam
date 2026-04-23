package com.vamigo.seat;

import com.vamigo.domain.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long>, JpaSpecificationExecutor<Seat> {

    List<Seat> findByPassengerIdOrderByDepartureTimeAscIdAsc(Long passengerId);

    boolean existsByIdAndPassengerId(Long id, Long passengerId);

    @Query("SELECT s FROM Seat s WHERE s.status = :status AND s.departureTime < :cutoff")
    List<Seat> findByStatusAndDepartureTimeBefore(Status status, Instant cutoff);
}
