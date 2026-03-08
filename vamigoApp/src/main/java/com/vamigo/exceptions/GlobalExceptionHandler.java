package com.vamigo.exceptions;

import com.vamigo.auth.exception.EmailAlreadyVerifiedException;
import com.vamigo.contact.ContactRateLimitException;
import com.vamigo.auth.exception.InvalidTokenException;
import com.vamigo.auth.exception.VerificationCooldownException;
import com.vamigo.email.EmailSendException;
import com.vamigo.location.NoSuchLocationException;
import com.vamigo.messaging.exception.ConversationNotFoundException;
import com.vamigo.messaging.exception.NotParticipantException;
import com.vamigo.messaging.exception.SelfConversationException;
import com.vamigo.report.exception.AlreadyReportedException;
import com.vamigo.review.exception.BookingNotReviewableException;
import com.vamigo.review.exception.ReviewAlreadySubmittedException;
import com.vamigo.review.exception.ReviewDeadlinePassedException;
import com.vamigo.review.exception.ReviewNotAllowedException;
import com.vamigo.user.exception.AvatarKeyMismatchException;
import com.vamigo.user.exception.AvatarNotUploadedException;
import com.vamigo.user.exception.DuplicateEmailException;
import com.vamigo.user.exception.InvalidAvatarContentTypeException;
import com.vamigo.user.exception.NoSuchUserException;
import com.vamigo.user.exception.StorageUnavailableException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler({NoSuchRideException.class, NoSuchSeatException.class})
    public ProblemDetail handleNotFoundEntity(HttpServletRequest request, RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({MissingETagHeaderException.class, ETagMismatchException.class})
    public ProblemDetail handleMissingETagHeaderException(HttpServletRequest request, RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(NoSuchUserException.class)
    public ProblemDetail handleNoSuchUserException(HttpServletRequest request, NoSuchUserException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({RideFullException.class, AlreadyBookedException.class, SegmentFullException.class,
            RideHasBookingsException.class, InvalidBookingTransitionException.class, InsufficientSeatsException.class})
    public ProblemDetail handleBookingConflictException(HttpServletRequest request, RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({NotRideDriverException.class, NotSeatPassengerException.class})
    public ProblemDetail handleOwnershipException(HttpServletRequest request, RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InvalidBookingSegmentException.class)
    public ProblemDetail handleInvalidBookingSegmentException(HttpServletRequest request, InvalidBookingSegmentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ProblemDetail handleBookingNotFoundException(HttpServletRequest request, BookingNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(RideNotBookableException.class)
    public ProblemDetail handleRideNotBookableException(HttpServletRequest request, RideNotBookableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ProblemDetail handleInvalidTokenException(HttpServletRequest request, InvalidTokenException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentialsException(HttpServletRequest request, BadCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    @ExceptionHandler(LockedException.class)
    public ProblemDetail handleLockedException(HttpServletRequest request, LockedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN,
                "Account is temporarily locked due to too many failed login attempts. Please try again later.");
        problem.setProperty("errorCode", "ACCOUNT_TEMPORARILY_LOCKED");
        return problem;
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmailException(HttpServletRequest request, DuplicateEmailException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ProblemDetail handleEmailAlreadyVerified(HttpServletRequest request, EmailAlreadyVerifiedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(VerificationCooldownException.class)
    public ProblemDetail handleVerificationCooldown(HttpServletRequest request, VerificationCooldownException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(ContactRateLimitException.class)
    public ProblemDetail handleContactRateLimitException(HttpServletRequest request, ContactRateLimitException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
    }

    @ExceptionHandler(EmailSendException.class)
    public ProblemDetail handleEmailSendException(HttpServletRequest request, EmailSendException ex) {
        LOGGER.error("Email send failure", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to send email. Please try again later.");
    }

    @ExceptionHandler(NoSuchLocationException.class)
    public ProblemDetail handleNoSuchLocationException(HttpServletRequest request, com.vamigo.location.NoSuchLocationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateExternalEntityException.class)
    public ProblemDetail handleDuplicateExternalEntityException(HttpServletRequest request, DuplicateExternalEntityException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(GeocodingException.class)
    public ProblemDetail handleGeocodingException(HttpServletRequest request, GeocodingException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ProblemDetail handleConversationNotFoundException(HttpServletRequest request, ConversationNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SelfConversationException.class)
    public ProblemDetail handleMessagingBadRequest(HttpServletRequest request, SelfConversationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NotParticipantException.class)
    public ProblemDetail handleNotParticipantException(HttpServletRequest request, NotParticipantException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler({CannotCreateRideException.class, CannotBookException.class, CannotBookOwnRideException.class})
    public ProblemDetail handleCapabilityException(HttpServletRequest request, RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(ExternalRideNotBookableException.class)
    public ProblemDetail handleExternalRideNotBookableException(HttpServletRequest request, ExternalRideNotBookableException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(FacebookBotMissingException.class)
    public ResponseEntity<ProblemDetail> handleFacebookBotMissingException(HttpServletRequest request, FacebookBotMissingException ex) {
        LOGGER.error("Facebook bot account not found - DataInitializer may have failed", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error"));
    }

    @ExceptionHandler(ReviewAlreadySubmittedException.class)
    public ProblemDetail handleReviewAlreadySubmittedException(HttpServletRequest request, ReviewAlreadySubmittedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({ReviewDeadlinePassedException.class, BookingNotReviewableException.class})
    public ProblemDetail handleReviewBadRequestException(HttpServletRequest request, RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ReviewNotAllowedException.class)
    public ProblemDetail handleReviewNotAllowedException(HttpServletRequest request, ReviewNotAllowedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(AlreadyReportedException.class)
    public ProblemDetail handleAlreadyReportedException(HttpServletRequest request, AlreadyReportedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(AvatarKeyMismatchException.class)
    public ProblemDetail handleAvatarKeyMismatchException(HttpServletRequest request, AvatarKeyMismatchException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(InvalidAvatarContentTypeException.class)
    public ProblemDetail handleInvalidAvatarContentTypeException(HttpServletRequest request, InvalidAvatarContentTypeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
    }

    @ExceptionHandler(AvatarNotUploadedException.class)
    public ProblemDetail handleAvatarNotUploadedException(HttpServletRequest request, AvatarNotUploadedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(StorageUnavailableException.class)
    public ProblemDetail handleStorageUnavailableException(HttpServletRequest request, StorageUnavailableException ex) {
        LOGGER.error("Storage service unavailable", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE,
                "Storage service is temporarily unavailable. Please try again later.");
    }
}
