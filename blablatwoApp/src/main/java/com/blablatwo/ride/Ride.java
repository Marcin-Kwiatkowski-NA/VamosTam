package com.blablatwo.ride;

import com.blablatwo.domain.AbstractTrip;
import com.blablatwo.domain.Status;
import com.blablatwo.user.UserAccount;
import com.blablatwo.vehicle.Vehicle;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Ride extends AbstractTrip {

    @ManyToOne
    private UserAccount driver;

    private int availableSeats;

    @Column(name = "price_per_seat", precision = 10, scale = 2)
    private BigDecimal pricePerSeat;

    @ManyToOne
    private Vehicle vehicle;

    @ManyToMany
    private List<UserAccount> passengers;

    public RideStatus computeRideStatus() {
        return switch (getStatus()) {
            case CANCELLED -> RideStatus.CANCELLED;
            case BANNED -> RideStatus.BANNED;
            case ACTIVE -> {
                boolean departed = getTimeSlot().toLocalDateTime().isBefore(LocalDateTime.now());
                boolean hasPassengers = passengers != null && !passengers.isEmpty();
                if (departed) {
                    yield hasPassengers ? RideStatus.COMPLETED : RideStatus.EXPIRED;
                }
                boolean full = hasPassengers && passengers.size() >= availableSeats;
                yield full ? RideStatus.FULL : RideStatus.OPEN;
            }
        };
    }
}
