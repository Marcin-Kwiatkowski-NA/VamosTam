package com.blablatwo.ride;

import com.blablatwo.ride.dto.BookRideRequest;
import com.blablatwo.ride.dto.BookingResponseDto;

import java.util.List;

public interface BookingService {

    BookingResponseDto createBooking(Long rideId, Long passengerId, BookRideRequest request);

    BookingResponseDto getBooking(Long rideId, Long bookingId);

    BookingResponseDto confirmBooking(Long rideId, Long bookingId, Long driverId);

    BookingResponseDto rejectBooking(Long rideId, Long bookingId, Long driverId);

    BookingResponseDto cancelBooking(Long rideId, Long bookingId, Long userId, String reason);

    List<BookingResponseDto> getBookingsForRide(Long rideId, Long driverId);

    List<BookingResponseDto> getMyBookings(Long passengerId);
}
