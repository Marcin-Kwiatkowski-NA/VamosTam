package com.vamigo.review;

import com.vamigo.AbstractFullStackTest;
import com.vamigo.ride.BookingStatus;
import com.vamigo.ride.Ride;
import com.vamigo.ride.RideBooking;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserProfile;
import com.vamigo.util.IntegrationFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * {@link ReviewPublishScheduler} is wired to a 1-second cron in tests
 * (see {@code review.publish-cron} in application-test.properties). A PENDING review
 * with a past {@code publishedAt} flips to PUBLISHED and bumps the subject's
 * {@code ratingSum}/{@code ratingCount}.
 *
 * <p>Fail-fast guard: if this test ever goes green after swapping the cron to an impossible
 * value (e.g. {@code 0 0 0 1 1 ? 2099}), the scheduler isn't firing and the green is false.
 * See the verification cron-sanity note in the test plan.
 */
class ReviewPublishSchedulerIT extends AbstractFullStackTest {

    @Autowired IntegrationFixtures fx;
    @Autowired ReviewRepository reviewRepository;

    @Test
    void pendingReviewWithPastPublishedAt_isPublished_andRatingStatsAggregated() {
        UserAccount driver = fx.persistUser();
        UserAccount passenger = fx.persistUser();
        Ride ride = fx.persistSimpleRide(driver);
        RideBooking booking = fx.persistBooking(ride, passenger, BookingStatus.CONFIRMED);

        Review review = Review.builder()
                .booking(booking)
                .author(passenger)
                .subject(driver)
                .authorRole(ReviewRole.PASSENGER)
                .stars(5)
                .comment("Great driver")
                .tags(new HashSet<>())
                .status(ReviewStatus.PENDING)
                .createdAt(Instant.now().minus(1, ChronoUnit.MINUTES))
                .publishedAt(Instant.now().minus(30, ChronoUnit.SECONDS))
                .deadlineAt(Instant.now().plus(7, ChronoUnit.DAYS))
                .build();
        Review saved = reviewRepository.saveAndFlush(review);

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Review loaded = reviewRepository.findById(saved.getId()).orElseThrow();
            assertThat(loaded.getStatus()).isEqualTo(ReviewStatus.PUBLISHED);
        });

        await().atMost(Duration.ofSeconds(2)).untilAsserted(() -> {
            UserProfile subject = fx.userProfileRepository().findById(driver.getId()).orElseThrow();
            assertThat(subject.getStats().getRatingCount()).isEqualTo(1);
            assertThat(subject.getStats().getRatingSum()).isEqualTo(5);
        });
    }
}
