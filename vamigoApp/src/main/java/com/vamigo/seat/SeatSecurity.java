package com.vamigo.seat;

import com.vamigo.auth.AppPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("seatSecurity")
@RequiredArgsConstructor
public class SeatSecurity {

    private final SeatRepository seatRepository;

    public boolean isPassenger(AppPrincipal principal, Long seatId) {
        return seatRepository.existsByIdAndPassengerId(seatId, principal.userId());
    }
}
