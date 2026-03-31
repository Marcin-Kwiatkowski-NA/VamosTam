package com.vamigo.ride;

import com.vamigo.domain.AbstractTrip;
import com.vamigo.location.Location;
import com.vamigo.user.UserAccount;
import com.vamigo.vehicle.Vehicle;
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
import java.time.Instant;
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

    @Column(name = "price_per_seat", precision = 6, scale = 2)
    private BigDecimal pricePerSeat;

    @ManyToOne
    private Vehicle vehicle;

    @Column(name = "is_time_approximate", nullable = false)
    private boolean isTimeApproximate;

    @Column(name = "departure_time")
    private Instant departureTime;

    @Column(name = "auto_approve", nullable = false)
    private boolean autoApprove;

    @Column(name = "door_to_door", nullable = false)
    private boolean doorToDoor;

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopOrder ASC")
    private List<RideStop> stops;

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RideBooking> bookings;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "estimated_arrival_at")
    private Instant estimatedArrivalAt;

    public Location getOrigin() {
        return stops.get(0).getLocation();
    }

    public Location getDestination() {
        return stops.get(stops.size() - 1).getLocation();
    }

    public List<RideBooking> getActiveBookings() {
        if (bookings == null) return List.of();
        return bookings.stream().filter(RideBooking::isActive).toList();
    }

    public int getAvailableSeatsForSegment(int boardOrder, int alightOrder) {
        List<RideBooking> active = getActiveBookings();
        if (active.isEmpty()) return totalSeats;
        int maxOccupied = 0;
        for (int leg = boardOrder; leg < alightOrder; leg++) {
            int occupied = 0;
            for (RideBooking b : active) {
                if (leg >= b.getBoardStop().getStopOrder()
                        && leg < b.getAlightStop().getStopOrder()) {
                    occupied += b.getSeatCount();
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

    public RideStatus getRideStatus() {
        return switch (getStatus()) {
            case ACTIVE -> getMinAvailableSeats() <= 0 ? RideStatus.FULL : RideStatus.OPEN;
            case COMPLETED -> RideStatus.COMPLETED;
            case EXPIRED -> RideStatus.EXPIRED;
            case CANCELLED -> RideStatus.CANCELLED;
            case BANNED -> RideStatus.BANNED;
        };
    }

    /**
     * Sums leg prices for stops in [{@code boardOrder}, {@code alightOrder}).
     * Returns {@code null} if any leg in the range has no price set.
     */
    public BigDecimal getSegmentPrice(int boardOrder, int alightOrder) {
        if (stops == null) return null;
        BigDecimal sum = BigDecimal.ZERO;
        for (RideStop stop : stops) {
            int order = stop.getStopOrder();
            if (order >= boardOrder && order < alightOrder) {
                if (stop.getLegPrice() == null) return null;
                sum = sum.add(stop.getLegPrice());
            }
        }
        return sum;
    }

    public List<RideBooking> getConfirmedBookings() {
        if (bookings == null) return List.of();
        return bookings.stream()
                .filter(b -> b.getStatus() == BookingStatus.CONFIRMED)
                .toList();
    }
}
