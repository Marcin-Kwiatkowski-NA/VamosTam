package com.blablatwo.exceptions;

import com.blablatwo.auth.exception.InvalidTokenException;
import com.blablatwo.messaging.exception.ConversationNotFoundException;
import com.blablatwo.messaging.exception.ExternalRideException;
import com.blablatwo.messaging.exception.InvalidDriverException;
import com.blablatwo.messaging.exception.NotBookedOnRideException;
import com.blablatwo.messaging.exception.NotParticipantException;
import com.blablatwo.messaging.exception.SelfConversationException;
import com.blablatwo.traveler.DuplicateEmailException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoSuchRideException.class)
    public ProblemDetail handleNoSuchRideException(HttpServletRequest request, NoSuchRideException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({MissingETagHeaderException.class, ETagMismatchException.class})
    public ProblemDetail handleMissingETagHeaderException(HttpServletRequest request, RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PRECONDITION_FAILED, ex.getMessage());
    }

    @ExceptionHandler(NoSuchTravelerException.class)
    public ProblemDetail handleNoSuchTravelerException(HttpServletRequest request, NoSuchTravelerException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({RideFullException.class, AlreadyBookedException.class})
    public ProblemDetail handleBookingConflictException(HttpServletRequest request, RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
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
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password");
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ProblemDetail handleDuplicateEmailException(HttpServletRequest request, DuplicateEmailException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NoSuchCityException.class)
    public ProblemDetail handleNoSuchCityException(HttpServletRequest request, NoSuchCityException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(DuplicateExternalRideException.class)
    public ProblemDetail handleDuplicateExternalRideException(HttpServletRequest request, DuplicateExternalRideException ex) {
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

    @ExceptionHandler({ExternalRideException.class, InvalidDriverException.class, SelfConversationException.class, NotBookedOnRideException.class})
    public ProblemDetail handleMessagingBadRequest(HttpServletRequest request, RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(NotParticipantException.class)
    public ProblemDetail handleNotParticipantException(HttpServletRequest request, NotParticipantException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }
}
