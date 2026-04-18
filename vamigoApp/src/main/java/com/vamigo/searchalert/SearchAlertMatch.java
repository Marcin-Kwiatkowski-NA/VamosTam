package com.vamigo.searchalert;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "search_alert_match", indexes = {
        @Index(name = "idx_sam_push_created", columnList = "push_sent, created_at"),
        @Index(name = "idx_sam_email_exact", columnList = "email_sent, exact_match"),
        @Index(name = "idx_sam_saved_search", columnList = "saved_search_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchAlertMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "saved_search_id", nullable = false)
    private SavedSearch savedSearch;

    @Column(name = "ride_id")
    private Long rideId;

    @Column(name = "seat_id")
    private Long seatId;

    @Builder.Default
    @Column(name = "exact_match", nullable = false)
    private boolean exactMatch = false;

    @Column(name = "origin_stop_name", length = 120)
    private String originStopName;

    @Column(name = "origin_distance_m")
    private Integer originDistanceM;

    @Column(name = "destination_stop_name", length = 120)
    private String destinationStopName;

    @Column(name = "destination_distance_m")
    private Integer destinationDistanceM;

    @Builder.Default
    @Column(name = "push_sent", nullable = false)
    private boolean pushSent = false;

    @Builder.Default
    @Column(name = "email_sent", nullable = false)
    private boolean emailSent = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
