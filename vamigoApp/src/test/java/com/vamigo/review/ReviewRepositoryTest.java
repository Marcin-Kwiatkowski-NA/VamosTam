package com.vamigo.review;

import com.vamigo.AbstractIntegrationTest;
import com.vamigo.domain.Status;
import com.vamigo.location.Location;
import com.vamigo.ride.BookingStatus;
import com.vamigo.ride.Ride;
import com.vamigo.ride.RideBooking;
import com.vamigo.user.UserAccount;
import com.vamigo.vehicle.Vehicle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
class ReviewRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private ReviewRepository repository;

    private UserAccount author;
    private UserAccount subject;
    private RideBooking booking;

    @BeforeEach
    void setUp() {
        Location origin = em.persistAndFlush(anOriginLocation().id(null).build());
        Location destination = em.persistAndFlush(aDestinationLocation().id(null).build());

        subject = em.persistAndFlush(anActiveUserAccount().email("driver@example.com").build());
        em.persistAndFlush(aUserProfile(subject).build());

        author = em.persistAndFlush(anActiveUserAccount().email("passenger@example.com").build());
        em.persistAndFlush(aUserProfile(author).displayName("Passenger").build());

        Vehicle vehicle = em.persistAndFlush(aTesla().id(null).owner(subject).build());

        Ride ride = aRide(origin, destination)
                .id(null).driver(subject).vehicle(vehicle)
                .status(Status.COMPLETED).stops(new java.util.ArrayList<>()).build();
        ride.getStops().addAll(buildStops(ride, origin, destination));
        ride = em.persistAndFlush(ride);

        booking = em.persistAndFlush(aBooking(ride, author).id(null).status(BookingStatus.CONFIRMED).build());
    }

    @Nested
    @DisplayName("Lookup review by booking and author")
    class FindByBookingAndAuthorTests {

        @Test
        @DisplayName("Returns the review when the author wrote one for the booking")
        void returnsReviewWhenAuthorMatches() {
            Review r = em.persistAndFlush(aReview(booking, author, subject).build());
            em.clear();

            Optional<Review> found = repository.findByBookingIdAndAuthorId(booking.getId(), author.getId());

            assertThat(found).isPresent()
                    .get().extracting(Review::getId).isEqualTo(r.getId());
        }

        @Test
        @DisplayName("Returns empty when the requesting user is not the review's author")
        void returnsEmptyWhenAuthorDoesNotMatch() {
            em.persistAndFlush(aReview(booking, author, subject).build());
            em.clear();

            assertThat(repository.findByBookingIdAndAuthorId(booking.getId(), subject.getId())).isEmpty();
        }
    }

    @Nested
    @DisplayName("List published reviews visible for a subject")
    class FindPublishedTests {

        @Test
        @DisplayName("Returns only reviews that are published and not in the future")
        void returnsOnlyPublishedReviewsForSubject() {
            em.persistAndFlush(aReview(booking, author, subject)
                    .status(ReviewStatus.PUBLISHED).publishedAt(Instant.now().minusSeconds(5)).build());
            em.clear();

            Page<Review> page = repository.findPublishedReviewsForSubject(
                    subject.getId(), Instant.now(), PageRequest.of(0, 10));

            assertThat(page.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("Excludes reviews that are still pending publication")
        void excludesPendingReviews() {
            em.persistAndFlush(aReview(booking, author, subject)
                    .status(ReviewStatus.PENDING).publishedAt(Instant.now().plusSeconds(60)).build());
            em.clear();

            Page<Review> page = repository.findPublishedReviewsForSubject(
                    subject.getId(), Instant.now(), PageRequest.of(0, 10));

            assertThat(page.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("List pending reviews ready to be published")
    class FindPendingReadyTests {

        @Test
        @DisplayName("Returns pending reviews whose scheduled publishedAt is in the past")
        void returnsPendingReviewsWithPublishedAtBeforeNow() {
            em.persistAndFlush(aReview(booking, author, subject)
                    .status(ReviewStatus.PENDING).publishedAt(Instant.now().minus(1, ChronoUnit.HOURS)).build());
            em.clear();

            List<Review> ready = repository.findPendingReviewsReadyToPublish(Instant.now());

            assertThat(ready).hasSize(1);
        }

        @Test
        @DisplayName("Excludes reviews that are already in PUBLISHED status")
        void excludesAlreadyPublished() {
            em.persistAndFlush(aReview(booking, author, subject)
                    .status(ReviewStatus.PUBLISHED).publishedAt(Instant.now().minus(1, ChronoUnit.HOURS)).build());
            em.clear();

            assertThat(repository.findPendingReviewsReadyToPublish(Instant.now())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Unique constraint on (booking, author)")
    class UniquenessTests {

        @Test
        @DisplayName("Prevents the same author from writing two reviews for one booking")
        void preventsDuplicateReviewByAuthorForBooking() {
            em.persistAndFlush(aReview(booking, author, subject).build());

            assertThatThrownBy(() -> em.persistAndFlush(aReview(booking, author, subject).build()))
                    .isInstanceOfAny(DataIntegrityViolationException.class,
                            jakarta.persistence.PersistenceException.class);
        }
    }

    @Nested
    @DisplayName("Aggregate rating distribution for a subject")
    class FindRatingDistributionTests {

        @Test
        @DisplayName("Aggregates published review counts into 1-to-5 star buckets")
        void aggregatesAcrossAllStarBuckets() {
            for (int stars = 1; stars <= 5; stars++) {
                RideBooking b = em.persistAndFlush(
                        aBooking(booking.getRide(), author).id(null).build());
                UserAccount uniqueAuthor = em.persistAndFlush(
                        anActiveUserAccount().email("rev-" + stars + "@example.com").build());
                em.persistAndFlush(aReview(b, uniqueAuthor, subject)
                        .stars(stars)
                        .status(ReviewStatus.PUBLISHED)
                        .publishedAt(Instant.now().minusSeconds(5))
                        .build());
            }
            em.clear();

            List<ReviewRepository.StarsCount> distribution =
                    repository.findRatingDistribution(subject.getId(), Instant.now());

            Map<Integer, Long> byStars = distribution.stream()
                    .collect(Collectors.toMap(ReviewRepository.StarsCount::getStars,
                            ReviewRepository.StarsCount::getCount));
            assertThat(byStars).containsOnlyKeys(1, 2, 3, 4, 5);
            assertThat(byStars.values()).allSatisfy(c -> assertThat(c).isEqualTo(1L));
        }
    }
}
