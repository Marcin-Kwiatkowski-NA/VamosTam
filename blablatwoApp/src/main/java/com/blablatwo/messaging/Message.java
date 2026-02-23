package com.blablatwo.messaging;

import com.blablatwo.user.UserAccount;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(indexes = {
    @Index(name = "idx_msg_conv_created", columnList = "conversation_id, created_at"),
    @Index(name = "idx_msg_status_update", columnList = "conversation_id, sender_id, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserAccount sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false)
    @Builder.Default
    private MessageType messageType = MessageType.USER;

    @NotBlank
    @Size(max = 2000)
    @Column(nullable = false, length = 2000)
    private String body;

    @Column(nullable = false, updatable = false, name = "created_at")
    private Instant createdAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public MessageStatus getDerivedStatus() {
        return MessageStatus.fromTimestamps(deliveredAt, readAt);
    }
}
