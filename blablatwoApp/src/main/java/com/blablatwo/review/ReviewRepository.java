package com.blablatwo.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByBookingIdAndAuthorId(Long bookingId, Long authorId);

    Optional<Review> findByBookingIdAndSubjectId(Long bookingId, Long subjectId);

    @Query("""
        SELECT r FROM Review r
        JOIN FETCH r.author
        WHERE r.subject.id = :subjectId
        AND r.status = com.blablatwo.review.ReviewStatus.PUBLISHED
        AND r.publishedAt <= :now
        ORDER BY r.createdAt DESC
        """)
    Page<Review> findPublishedReviewsForSubject(
            @Param("subjectId") Long subjectId,
            @Param("now") Instant now,
            Pageable pageable);

    @Query("""
        SELECT r FROM Review r
        WHERE r.status = com.blablatwo.review.ReviewStatus.PENDING
        AND r.publishedAt <= :now
        """)
    List<Review> findPendingReviewsReadyToPublish(@Param("now") Instant now);

    boolean existsByBookingIdAndAuthorId(Long bookingId, Long authorId);

    interface StarsCount {
        int getStars();
        long getCount();
    }

    @Query("""
        SELECT r.stars AS stars, COUNT(r) AS count
        FROM Review r
        WHERE r.subject.id = :subjectId
        AND r.status = com.blablatwo.review.ReviewStatus.PUBLISHED
        AND r.publishedAt <= :now
        GROUP BY r.stars
        """)
    List<StarsCount> findRatingDistribution(
            @Param("subjectId") Long subjectId,
            @Param("now") Instant now);
}
