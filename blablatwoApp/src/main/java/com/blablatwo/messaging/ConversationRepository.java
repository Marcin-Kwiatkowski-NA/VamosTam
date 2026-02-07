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
        JOIN FETCH c.participantA
        JOIN FETCH c.participantB
        WHERE c.topicKey = :topicKey
        AND c.participantA.id = :participantAId
        AND c.participantB.id = :participantBId
        """)
    Optional<Conversation> findByTopicKeyAndParticipants(
        @Param("topicKey") String topicKey,
        @Param("participantAId") Long participantAId,
        @Param("participantBId") Long participantBId);

    @Query("""
        SELECT c FROM Conversation c
        JOIN FETCH c.participantA
        JOIN FETCH c.participantB
        WHERE c.participantA.id = :userId OR c.participantB.id = :userId
        ORDER BY c.updatedAt DESC
        """)
    List<Conversation> findByParticipantId(
        @Param("userId") Long userId,
        Pageable pageable);

    @Query("""
        SELECT c FROM Conversation c
        JOIN FETCH c.participantA
        JOIN FETCH c.participantB
        WHERE (c.participantA.id = :userId OR c.participantB.id = :userId)
        AND c.updatedAt > :since
        ORDER BY c.updatedAt DESC
        """)
    List<Conversation> findByParticipantIdAndUpdatedAtAfter(
        @Param("userId") Long userId,
        @Param("since") Instant since,
        Pageable pageable);
}
