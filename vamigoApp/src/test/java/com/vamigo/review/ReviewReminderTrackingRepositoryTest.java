package com.vamigo.review;

import com.vamigo.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ReviewReminderTrackingRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ReviewReminderTrackingRepository repository;

    @Nested
    @DisplayName("Check whether a reminder has already been sent")
    class ExistsTests {

        @Test
        @DisplayName("Returns true when booking, user and reminder type all match a sent row")
        void returnsTrueWhenAllThreeMatch() {
            em.persistAndFlush(ReviewReminderTracking.builder()
                    .bookingId(10L).userId(100L)
                    .type(ReviewReminderTracking.ReminderType.COMPLETION)
                    .sentAt(Instant.now()).build());
            em.clear();

            assertThat(repository.existsByBookingIdAndUserIdAndType(
                    10L, 100L, ReviewReminderTracking.ReminderType.COMPLETION)).isTrue();
        }

        @Test
        @DisplayName("Returns false when a different reminder type was sent for the booking")
        void returnsFalseWhenTypeDiffers() {
            em.persistAndFlush(ReviewReminderTracking.builder()
                    .bookingId(10L).userId(100L)
                    .type(ReviewReminderTracking.ReminderType.COMPLETION)
                    .sentAt(Instant.now()).build());
            em.clear();

            assertThat(repository.existsByBookingIdAndUserIdAndType(
                    10L, 100L, ReviewReminderTracking.ReminderType.NUDGE)).isFalse();
        }

        @Test
        @DisplayName("Returns false when the booking id differs from the tracked row")
        void returnsFalseWhenBookingDiffers() {
            em.persistAndFlush(ReviewReminderTracking.builder()
                    .bookingId(10L).userId(100L)
                    .type(ReviewReminderTracking.ReminderType.COMPLETION)
                    .sentAt(Instant.now()).build());
            em.clear();

            assertThat(repository.existsByBookingIdAndUserIdAndType(
                    99L, 100L, ReviewReminderTracking.ReminderType.COMPLETION)).isFalse();
        }
    }
}
