package com.blablatwo.messaging;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    @Query("""
        SELECT c FROM Conversation c
        JOIN FETCH c.ride r
        JOIN FETCH r.origin
        JOIN FETCH r.destination
        JOIN FETCH c.driver
        JOIN FETCH c.passenger
        WHERE c.ride.id = :rideId
        AND c.driver.id = :driverId
        AND c.passenger.id = :passengerId
        """)
    Optional<Conversation> findByRideIdAndDriverIdAndPassengerId(
        @Param("rideId") Long rideId,
        @Param("driverId") Long driverId,
        @Param("passengerId") Long passengerId);

    @Query("""
        SELECT c FROM Conversation c
        JOIN FETCH c.ride r
        JOIN FETCH r.origin
        JOIN FETCH r.destination
        JOIN FETCH c.driver
        JOIN FETCH c.passenger
        WHERE c.driver.id = :travelerId OR c.passenger.id = :travelerId
        ORDER BY c.updatedAt DESC
        """)
    List<Conversation> findByParticipantId(
        @Param("travelerId") Long travelerId,
        Pageable pageable);

    @Query("""
        SELECT c FROM Conversation c
        JOIN FETCH c.ride r
        JOIN FETCH r.origin
        JOIN FETCH r.destination
        JOIN FETCH c.driver
        JOIN FETCH c.passenger
        WHERE (c.driver.id = :travelerId OR c.passenger.id = :travelerId)
        AND c.updatedAt > :since
        ORDER BY c.updatedAt DESC
        """)
    List<Conversation> findByParticipantIdAndUpdatedAtAfter(
        @Param("travelerId") Long travelerId,
        @Param("since") Instant since,
        Pageable pageable);
}
