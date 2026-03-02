package com.vamigo.messaging;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    List<Message> findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            UUID conversationId, Instant before, Pageable pageable);

    List<Message> findByConversationIdAndCreatedAtAfterOrderByCreatedAtAsc(
            UUID conversationId, Instant since, Pageable pageable);

    void deleteByConversationIdIn(Collection<UUID> conversationIds);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Message m
            SET m.deliveredAt = :now
            WHERE m.conversation.id = :convId
              AND m.sender.id = :senderId
              AND m.deliveredAt IS NULL
              AND m.createdAt <= :upTo
            """)
    int markDelivered(UUID convId, Long senderId, Instant upTo, Instant now);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Message m
            SET m.readAt = :now, m.deliveredAt = COALESCE(m.deliveredAt, :now)
            WHERE m.conversation.id = :convId
              AND m.sender.id = :senderId
              AND m.readAt IS NULL
              AND m.createdAt <= :upTo
            """)
    int markRead(UUID convId, Long senderId, Instant upTo, Instant now);
}
