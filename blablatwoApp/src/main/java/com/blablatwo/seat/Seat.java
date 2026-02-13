package com.blablatwo.seat;

import com.blablatwo.city.City;
import com.blablatwo.domain.AbstractTrip;
import com.blablatwo.user.UserAccount;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Seat extends AbstractTrip {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_city_id", nullable = false)
    private City origin;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_city_id", nullable = false)
    private City destination;

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "is_approximate", nullable = false)
    private boolean isApproximate;

    @ManyToOne(optional = false)
    private UserAccount passenger;

    private int count;

    @Column(name = "price_willing_to_pay", precision = 10, scale = 2)
    private BigDecimal priceWillingToPay;

    public LocalDateTime getDepartureDateTime() {
        return departureDate.atTime(departureTime);
    }

    public SeatStatus computeSeatStatus() {
        return switch (getStatus()) {
            case CANCELLED -> SeatStatus.CANCELLED;
            case BANNED -> SeatStatus.BANNED;
            case ACTIVE -> {
                boolean departed = getDepartureDateTime().isBefore(LocalDateTime.now());
                yield departed ? SeatStatus.EXPIRED : SeatStatus.SEARCHING;
            }
        };
    }
}
