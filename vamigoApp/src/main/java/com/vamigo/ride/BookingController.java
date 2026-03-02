package com.vamigo.ride;

import com.vamigo.auth.AppPrincipal;
import com.vamigo.dto.CancellationRequest;
import com.vamigo.ride.dto.BookRideRequest;
import com.vamigo.ride.dto.BookingResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/rides/{rideId}/bookings")
    public ResponseEntity<BookingResponseDto> createBooking(
            @PathVariable Long rideId,
            @Valid @RequestBody BookRideRequest request,
            @AuthenticationPrincipal AppPrincipal principal) {
        BookingResponseDto booking = bookingService.createBooking(rideId, principal.userId(), request);
        URI location = URI.create("/rides/" + rideId + "/bookings/" + booking.id());
        return ResponseEntity.created(location).body(booking);
    }

    @GetMapping("/rides/{rideId}/bookings")
    public ResponseEntity<List<BookingResponseDto>> getBookingsForRide(
            @PathVariable Long rideId,
            @AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(bookingService.getBookingsForRide(rideId, principal.userId()));
    }

    @GetMapping("/rides/{rideId}/bookings/{bookingId}")
    public ResponseEntity<BookingResponseDto> getBooking(
            @PathVariable Long rideId,
            @PathVariable Long bookingId) {
        return ResponseEntity.ok(bookingService.getBooking(rideId, bookingId));
    }

    @PostMapping("/rides/{rideId}/bookings/{bookingId}/confirm")
    public ResponseEntity<BookingResponseDto> confirmBooking(
            @PathVariable Long rideId,
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(bookingService.confirmBooking(rideId, bookingId, principal.userId()));
    }

    @PostMapping("/rides/{rideId}/bookings/{bookingId}/reject")
    public ResponseEntity<BookingResponseDto> rejectBooking(
            @PathVariable Long rideId,
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(bookingService.rejectBooking(rideId, bookingId, principal.userId()));
    }

    @PostMapping("/rides/{rideId}/bookings/{bookingId}/cancel")
    public ResponseEntity<BookingResponseDto> cancelBooking(
            @PathVariable Long rideId,
            @PathVariable Long bookingId,
            @Valid @RequestBody CancellationRequest request,
            @AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(
                bookingService.cancelBooking(rideId, bookingId, principal.userId(), request.reason()));
    }

    @GetMapping("/me/bookings")
    public ResponseEntity<List<BookingResponseDto>> getMyBookings(
            @AuthenticationPrincipal AppPrincipal principal) {
        return ResponseEntity.ok(bookingService.getMyBookings(principal.userId()));
    }
}
