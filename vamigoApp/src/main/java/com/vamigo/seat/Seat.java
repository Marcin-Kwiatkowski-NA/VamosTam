package com.vamigo.seat;

import com.vamigo.domain.AbstractTrip;
import com.vamigo.location.Location;
import com.vamigo.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Seat extends AbstractTrip {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_location_id", nullable = false)
    private Location origin;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_location_id", nullable = false)
    private Location destination;

    @Column(name = "departure_time", nullable = false)
    private Instant departureTime;

    @Column(name = "is_time_approximate", nullable = false)
    private boolean isTimeApproximate;

    @ManyToOne(optional = false)
    private UserAccount passenger;

    private int count;

    @Column(name = "price_willing_to_pay", precision = 6, scale = 2)
    private BigDecimal priceWillingToPay;

    public SeatStatus computeSeatStatus() {
        return switch (getStatus()) {
            case COMPLETED -> SeatStatus.COMPLETED;
            case EXPIRED -> SeatStatus.EXPIRED;
            case CANCELLED -> SeatStatus.CANCELLED;
            case BANNED -> SeatStatus.BANNED;
            case ACTIVE -> {
                boolean departed = getDepartureTime().isBefore(Instant.now());
                yield departed ? SeatStatus.EXPIRED : SeatStatus.SEARCHING;
            }
        };
    }
}
