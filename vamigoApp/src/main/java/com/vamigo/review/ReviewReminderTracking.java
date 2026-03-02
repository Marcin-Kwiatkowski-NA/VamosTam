package com.vamigo.review;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(
    name = "review_reminder_tracking",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_reminder_booking_user_type",
        columnNames = {"booking_id", "user_id", "type"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReminderTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderType type;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    public enum ReminderType {
        COMPLETION,
        NUDGE
    }
}
