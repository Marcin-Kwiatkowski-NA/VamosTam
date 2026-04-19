package com.vamigo.searchalert;

import com.vamigo.user.UserAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "saved_search", indexes = {
        @Index(name = "idx_ss_user_active", columnList = "user_id, active"),
        @Index(name = "idx_ss_active_date", columnList = "active, departure_date"),
        @Index(name = "idx_ss_origin_coords", columnList = "origin_lat, origin_lon"),
        @Index(name = "idx_ss_dest_coords", columnList = "destination_lat, destination_lon")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
public class SavedSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(name = "origin_osm_id", nullable = false)
    private Long originOsmId;

    @Column(name = "origin_name", nullable = false, length = 200)
    private String originName;

    @Column(name = "origin_lat", nullable = false)
    private Double originLat;

    @Column(name = "origin_lon", nullable = false)
    private Double originLon;

    @Column(name = "destination_osm_id", nullable = false)
    private Long destinationOsmId;

    @Column(name = "destination_name", nullable = false, length = 200)
    private String destinationName;

    @Column(name = "destination_lat", nullable = false)
    private Double destinationLat;

    @Column(name = "destination_lon", nullable = false)
    private Double destinationLon;

    @Enumerated(EnumType.STRING)
    @Column(name = "search_type", nullable = false, length = 10)
    private SearchType searchType;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "min_available_seats")
    private Integer minAvailableSeats;

    @Column(length = 200)
    private String label;

    @Builder.Default
    @Column(name = "auto_created", nullable = false)
    private boolean autoCreated = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "last_push_sent_at")
    private Instant lastPushSentAt;

    @Column(name = "last_email_sent_at")
    private Instant lastEmailSentAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public void setActive(boolean active) {
        this.active = active;
    }

    public void recordPushSent(Instant when) {
        this.lastPushSentAt = when;
    }

    public void recordEmailSent(Instant when) {
        this.lastEmailSentAt = when;
    }
}
