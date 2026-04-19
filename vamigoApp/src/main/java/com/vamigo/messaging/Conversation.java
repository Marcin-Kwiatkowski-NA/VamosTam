package com.vamigo.messaging;

import com.vamigo.user.UserAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
public class Conversation {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @Column(name = "topic_key", nullable = false, length = 100)
    private String topicKey;

    @Column(name = "offer_key", length = 20)
    private String offerKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_a_id", nullable = false)
    private UserAccount participantA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participant_b_id", nullable = false)
    private UserAccount participantB;

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;

    @LastModifiedDate
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

    @Column(name = "participant_a_email_notified_at")
    private Instant participantAEmailNotifiedAt;

    @Column(name = "participant_b_email_notified_at")
    private Instant participantBEmailNotifiedAt;

    // Optimistic locking to prevent lost updates on concurrent sends
    @Version
    private int version;

    public void recordNewMessage(Message saved, Long senderId) {
        this.lastMessageId = saved.getId();
        this.lastMessageBody = saved.getBody();
        this.lastMessageCreatedAt = saved.getCreatedAt();
        this.lastMessageSenderId = senderId;
        if (participantA.getId().equals(senderId)) {
            this.participantBUnreadCount++;
            this.participantAUnreadCount = 0;
        } else {
            this.participantAUnreadCount++;
            this.participantBUnreadCount = 0;
        }
    }

    public void recordSystemMessage(Message saved, Long actorId) {
        this.lastMessageId = saved.getId();
        this.lastMessageBody = saved.getBody();
        this.lastMessageCreatedAt = saved.getCreatedAt();
        this.lastMessageSenderId = actorId;
    }

    public void markRead(Long participantId) {
        if (participantA.getId().equals(participantId)) {
            this.participantAUnreadCount = 0;
        } else if (participantB.getId().equals(participantId)) {
            this.participantBUnreadCount = 0;
        }
    }

    public void recordEmailNotified(Long participantId, Instant when) {
        if (participantA.getId().equals(participantId)) {
            this.participantAEmailNotifiedAt = when;
        } else if (participantB.getId().equals(participantId)) {
            this.participantBEmailNotifiedAt = when;
        }
    }
}
