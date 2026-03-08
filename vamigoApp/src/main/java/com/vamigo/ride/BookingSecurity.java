package com.vamigo.ride;

import com.vamigo.auth.AppPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("bookingSecurity")
@RequiredArgsConstructor
public class BookingSecurity {

    private final RideBookingRepository bookingRepository;
    private final RideRepository rideRepository;

    public boolean isRideDriver(AppPrincipal principal, Long rideId) {
        return rideRepository.existsByIdAndDriverId(rideId, principal.userId());
    }

    public boolean isBookingParticipant(AppPrincipal principal, Long rideId, Long bookingId) {
        return bookingRepository.isUserParticipant(rideId, bookingId, principal.userId());
    }
}
