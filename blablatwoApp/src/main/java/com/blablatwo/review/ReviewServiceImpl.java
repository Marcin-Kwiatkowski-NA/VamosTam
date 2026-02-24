package com.blablatwo.review;

import com.blablatwo.domain.PersonDisplayNameResolver;
import com.blablatwo.domain.Status;
import com.blablatwo.review.dto.PendingReviewDto;
import com.blablatwo.review.dto.ReviewResponseDto;
import com.blablatwo.review.dto.SubmitReviewRequest;
import com.blablatwo.review.event.ReviewSubmittedEvent;
import com.blablatwo.review.exception.BookingNotReviewableException;
import com.blablatwo.review.exception.ReviewAlreadySubmittedException;
import com.blablatwo.review.exception.ReviewDeadlinePassedException;
import com.blablatwo.review.exception.ReviewNotAllowedException;
import com.blablatwo.ride.BookingStatus;
import com.blablatwo.ride.Ride;
import com.blablatwo.ride.RideBooking;
import com.blablatwo.ride.RideBookingRepository;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);
    private static final Duration REVEAL_DELAY = Duration.ofDays(3);
    private static final Duration REVIEW_WINDOW = Duration.ofDays(14);

    private final ReviewRepository reviewRepository;
    private final RideBookingRepository bookingRepository;
    private final UserProfileRepository userProfileRepository;
    private final PersonDisplayNameResolver displayNameResolver;
    private final ReviewMapper reviewMapper;
    private final ApplicationEventPublisher eventPublisher;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             RideBookingRepository bookingRepository,
                             UserProfileRepository userProfileRepository,
                             PersonDisplayNameResolver displayNameResolver,
                             ReviewMapper reviewMapper,
                             ApplicationEventPublisher eventPublisher) {
        this.reviewRepository = reviewRepository;
        this.bookingRepository = bookingRepository;
        this.userProfileRepository = userProfileRepository;
        this.displayNameResolver = displayNameResolver;
        this.reviewMapper = reviewMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public ReviewResponseDto submitReview(Long bookingId, Long authorId, SubmitReviewRequest request) {
        RideBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotReviewableException(bookingId, "booking not found"));

        Ride ride = booking.getRide();

        // Must be CONFIRMED booking
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingNotReviewableException(bookingId, "booking is not confirmed");
        }

        // Ride must be COMPLETED
        if (ride.getStatus() != Status.COMPLETED) {
            throw new BookingNotReviewableException(bookingId, "ride is not completed");
        }

        // Author must be driver or passenger on this booking
        Long driverId = ride.getDriver().getId();
        Long passengerId = booking.getPassenger().getId();
        boolean isDriver = authorId.equals(driverId);
        boolean isPassenger = authorId.equals(passengerId);

        if (!isDriver && !isPassenger) {
            throw new ReviewNotAllowedException("user is not a participant of this booking");
        }

        // Check deadline (completedAt + 14 days)
        Instant deadline = ride.getCompletedAt().plus(REVIEW_WINDOW);
        if (Instant.now().isAfter(deadline)) {
            throw new ReviewDeadlinePassedException(bookingId);
        }

        // Check if already reviewed
        if (reviewRepository.existsByBookingIdAndAuthorId(bookingId, authorId)) {
            throw new ReviewAlreadySubmittedException(bookingId, authorId);
        }

        // Determine roles
        ReviewRole authorRole = isDriver ? ReviewRole.DRIVER : ReviewRole.PASSENGER;
        UserAccount author = isDriver ? ride.getDriver() : booking.getPassenger();
        UserAccount subject = isDriver ? booking.getPassenger() : ride.getDriver();

        Review review = Review.builder()
                .booking(booking)
                .author(author)
                .subject(subject)
                .authorRole(authorRole)
                .stars(request.stars())
                .comment(request.comment())
                .tags(request.tags() != null ? new HashSet<>(request.tags()) : new HashSet<>())
                .status(ReviewStatus.PENDING)
                .publishedAt(Instant.now().plus(REVEAL_DELAY))
                .deadlineAt(deadline)
                .build();

        Review saved = reviewRepository.save(review);

        eventPublisher.publishEvent(new ReviewSubmittedEvent(
                saved.getId(), bookingId, authorId, subject.getId(), ride.getId()));

        return reviewMapper.toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponseDto> getPublishedReviews(Long subjectId, int page, int size) {
        Page<Review> reviews = reviewRepository.findPublishedReviewsForSubject(
                subjectId, Instant.now(), PageRequest.of(page, size));
        return reviews.map(reviewMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingReviewDto> getPendingReviews(Long userId) {
        // Find all confirmed bookings on completed rides within the 14-day window
        // where the user hasn't submitted a review yet
        Instant cutoff = Instant.now().minus(REVIEW_WINDOW);
        List<RideBooking> confirmedBookings = bookingRepository.findConfirmedBookingsOnCompletedRides(userId, cutoff);

        List<PendingReviewDto> result = new ArrayList<>();

        for (RideBooking booking : confirmedBookings) {
            Ride ride = booking.getRide();
            if (ride.getCompletedAt() == null) continue;

            Instant deadline = ride.getCompletedAt().plus(REVIEW_WINDOW);
            if (Instant.now().isAfter(deadline)) continue;

            boolean isDriver = userId.equals(ride.getDriver().getId());
            Long peerId = isDriver ? booking.getPassenger().getId() : ride.getDriver().getId();

            // Skip if already reviewed
            if (reviewRepository.existsByBookingIdAndAuthorId(booking.getId(), userId)) continue;

            // Check if counterpart has submitted
            boolean counterpartSubmitted = reviewRepository.existsByBookingIdAndAuthorId(
                    booking.getId(), peerId);

            UserProfile peerProfile = userProfileRepository.findById(peerId).orElse(null);
            String peerName = peerProfile != null ? peerProfile.getDisplayName() : "User";
            String peerAvatarUrl = peerProfile != null ? peerProfile.getAvatarUrl() : null;

            result.add(PendingReviewDto.builder()
                    .bookingId(booking.getId())
                    .rideId(ride.getId())
                    .peerName(peerName)
                    .peerAvatarUrl(peerAvatarUrl)
                    .yourRole(isDriver ? ReviewRole.DRIVER : ReviewRole.PASSENGER)
                    .origin(ride.getOrigin().getName(null))
                    .destination(ride.getDestination().getName(null))
                    .departureDate(ride.getDepartureDate())
                    .deadlineAt(deadline)
                    .counterpartSubmitted(counterpartSubmitted)
                    .build());
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public long countPendingReviews(Long userId) {
        return getPendingReviews(userId).size();
    }
}
