package com.blablatwo.messaging;

import com.blablatwo.ride.Ride;
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
        name = "uk_conversation_ride_driver_passenger",
        columnNames = {"ride_id", "driver_id", "passenger_id"}),
    indexes = {
        @Index(name = "idx_conv_driver_updated", columnList = "driver_id, updated_at"),
        @Index(name = "idx_conv_passenger_updated", columnList = "passenger_id, updated_at"),
        @Index(name = "idx_conv_ride", columnList = "ride_id"),
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private UserAccount driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private UserAccount passenger;

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
    @Column(name = "driver_unread_count", nullable = false)
    @Builder.Default
    private int driverUnreadCount = 0;

    @Column(name = "passenger_unread_count", nullable = false)
    @Builder.Default
    private int passengerUnreadCount = 0;

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
