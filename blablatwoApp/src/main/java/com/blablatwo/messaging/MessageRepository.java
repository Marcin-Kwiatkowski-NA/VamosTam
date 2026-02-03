package com.blablatwo.messaging;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);

    List<Message> findByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            UUID conversationId, Instant before, Pageable pageable);

    List<Message> findByConversationIdAndCreatedAtAfterOrderByCreatedAtAsc(
            UUID conversationId, Instant since, Pageable pageable);
}
