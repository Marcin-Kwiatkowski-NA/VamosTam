package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.domain.AbstractTrip;
import com.blablatwo.user.UserAccount;
import com.blablatwo.vehicle.Vehicle;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    private int totalSeats;

    @Column(name = "price_per_seat", precision = 10, scale = 2)
    private BigDecimal pricePerSeat;

    @ManyToOne
    private Vehicle vehicle;

    @Column(name = "is_approximate", nullable = false)
    private boolean isApproximate;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    @Column(name = "departure_time")
    private LocalTime departureTime;

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopOrder ASC")
    private List<RideStop> stops;

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RideBooking> bookings;

    public City getOrigin() {
        return stops.get(0).getCity();
    }

    public City getDestination() {
        return stops.get(stops.size() - 1).getCity();
    }

    public LocalDateTime getDepartureDateTime() {
        return departureDate.atTime(departureTime);
    }

    /**
     * Available seats for a sub-segment [boardOrder, alightOrder).
     * <p>
     * For each leg in the range, counts how many bookings overlap that leg.
     * A booking overlaps leg L if: booking.boardStop.order <= L < booking.alightStop.order.
     * Returns totalSeats minus the maximum occupied count across all legs in range.
     */
    public int getAvailableSeatsForSegment(int boardOrder, int alightOrder) {
        if (bookings == null || bookings.isEmpty()) return totalSeats;
        int maxOccupied = 0;
        for (int leg = boardOrder; leg < alightOrder; leg++) {
            int occupied = 0;
            for (RideBooking b : bookings) {
                if (leg >= b.getBoardStop().getStopOrder()
                        && leg < b.getAlightStop().getStopOrder()) {
                    occupied++;
                }
            }
            maxOccupied = Math.max(maxOccupied, occupied);
        }
        return totalSeats - maxOccupied;
    }

    public int getMinAvailableSeats() {
        if (stops == null || stops.size() < 2) return totalSeats;
        return getAvailableSeatsForSegment(0, stops.size() - 1);
    }

    public RideStatus computeRideStatus() {
        return switch (getStatus()) {
            case CANCELLED -> RideStatus.CANCELLED;
            case BANNED -> RideStatus.BANNED;
            case ACTIVE -> {
                boolean departed = getDepartureDateTime().isBefore(LocalDateTime.now());
                boolean hasBookings = bookings != null && !bookings.isEmpty();
                if (departed) {
                    yield hasBookings ? RideStatus.COMPLETED : RideStatus.EXPIRED;
                }
                yield getMinAvailableSeats() <= 0 ? RideStatus.FULL : RideStatus.OPEN;
            }
        };
    }
}
