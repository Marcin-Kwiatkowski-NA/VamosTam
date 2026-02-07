package com.blablatwo.messaging;

import com.blablatwo.user.UserAccount;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "uk_conversation_topic_participants",
        columnNames = {"topic_key", "participant_a_id", "participant_b_id"}),
    indexes = {
        @Index(name = "idx_conv_topic", columnList = "topic_key"),
        @Index(name = "idx_conv_participant_a_updated", columnList = "participant_a_id, updated_at"),
        @Index(name = "idx_conv_participant_b_updated", columnList = "participant_b_id, updated_at"),
        @Index(name = "idx_conv_updated", columnList = "updated_at")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "topic_key", nullable = false, length = 100)
    private String topicKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_a_id", nullable = false)
    private UserAccount participantA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_b_id", nullable = false)
    private UserAccount participantB;

    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;

    @Column(nullable = false, name = "updated_at")
    private Instant updatedAt;

    // Denormalized last message preview (nullable fields)
    @Column(name = "last_message_id", nullable = true)
    private UUID lastMessageId;

    @Column(name = "last_message_body", length = 2000, nullable = true)
    private String lastMessageBody;

    @Column(name = "last_message_created_at", nullable = true)
    private Instant lastMessageCreatedAt;

    @Column(name = "last_message_sender_id", nullable = true)
    private Long lastMessageSenderId;

    // Denormalized unread counts (avoids N+1 on inbox listing)
    @Column(name = "participant_a_unread_count", nullable = false)
    @Builder.Default
    private int participantAUnreadCount = 0;

    @Column(name = "participant_b_unread_count", nullable = false)
    @Builder.Default
    private int participantBUnreadCount = 0;

    // Optimistic locking to prevent lost updates on concurrent sends
    @Version
    private int version;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
