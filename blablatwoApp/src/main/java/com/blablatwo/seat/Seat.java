package com.blablatwo.seat;

import com.blablatwo.domain.AbstractTrip;
import com.blablatwo.domain.Status;
import com.blablatwo.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Seat extends AbstractTrip {

    @ManyToOne(optional = false)
    private UserAccount passenger;

    private int count;

    @Column(name = "price_willing_to_pay", precision = 10, scale = 2)
    private BigDecimal priceWillingToPay;

    public SeatStatus computeSeatStatus() {
        return switch (getStatus()) {
            case CANCELLED -> SeatStatus.CANCELLED;
            case BANNED -> SeatStatus.BANNED;
            case ACTIVE -> {
                boolean departed = getTimeSlot().toLocalDateTime().isBefore(LocalDateTime.now());
                yield departed ? SeatStatus.EXPIRED : SeatStatus.SEARCHING;
            }
        };
    }
}
