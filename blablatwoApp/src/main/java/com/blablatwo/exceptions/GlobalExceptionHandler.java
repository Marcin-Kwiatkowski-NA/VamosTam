package com.blablatwo.exceptions;

import com.blablatwo.auth.exception.InvalidTokenException;
import com.blablatwo.messaging.exception.ConversationNotFoundException;
import com.blablatwo.messaging.exception.NotParticipantException;
import com.blablatwo.messaging.exception.SelfConversationException;
import com.blablatwo.user.exception.DuplicateEmailException;
import com.blablatwo.user.exception.NoSuchUserException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
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
            RideHasBookingsException.class})
    public ProblemDetail handleBookingConflictException(HttpServletRequest request, RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
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

    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmailException(HttpServletRequest request, DuplicateEmailException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NoSuchCityException.class)
    public ProblemDetail handleNoSuchCityException(HttpServletRequest request, NoSuchCityException ex) {
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

    @ExceptionHandler({CannotCreateRideException.class, CannotBookException.class})
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
}
