package com.vamigo.ride;

import com.vamigo.auth.AppPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("rideSecurity")
@RequiredArgsConstructor
public class RideSecurity {

    private final RideRepository rideRepository;

    public boolean isDriver(AppPrincipal principal, Long rideId) {
        return rideRepository.existsByIdAndDriverId(rideId, principal.userId());
    }
}
