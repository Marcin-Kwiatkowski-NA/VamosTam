package com.blablatwo.notification;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Slice<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    Optional<Notification> findByRecipientIdAndCollapseKeyAndReadAtIsNull(Long recipientId, String collapseKey);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = CURRENT_TIMESTAMP WHERE n.id = :id AND n.recipient.id = :userId AND n.readAt IS NULL")
    int markReadById(UUID id, Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.readAt = CURRENT_TIMESTAMP WHERE n.recipient.id = :userId AND n.readAt IS NULL")
    int markAllRead(Long userId);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.readAt IS NOT NULL AND n.readAt < :cutoff")
    int deleteReadOlderThan(Instant cutoff);
}
