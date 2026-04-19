package com.vamigo.review;

import com.vamigo.ride.RideBooking;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "uk_review_booking_author",
        columnNames = {"booking_id", "author_id"}),
    indexes = {
        @Index(name = "idx_review_subject_status_published", columnList = "subject_id, status, published_at"),
        @Index(name = "idx_review_booking", columnList = "booking_id"),
        @Index(name = "idx_review_status_published", columnList = "status, published_at")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private RideBooking booking;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private UserAccount author;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private UserAccount subject;

    @Enumerated(EnumType.STRING)
    @Column(name = "author_role", nullable = false, length = 20)
    private ReviewRole authorRole;

    @Column(nullable = false)
    private int stars;

    @Column(length = 500)
    private String comment;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "review_tag", joinColumns = @JoinColumn(name = "review_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "tag", length = 30)
    @Builder.Default
    private Set<ReviewTag> tags = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReviewStatus status = ReviewStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "deadline_at", nullable = false)
    private Instant deadlineAt;

    public Set<ReviewTag> getTags() {
        return tags == null ? Set.of() : Collections.unmodifiableSet(tags);
    }

    public void publish(Instant now) {
        this.status = ReviewStatus.PUBLISHED;
        this.publishedAt = now;
    }
}
