package com.vamigo.ride;

import com.vamigo.domain.AbstractTrip;
import com.vamigo.domain.Status;
import com.vamigo.domain.TimePrecision;
import com.vamigo.exceptions.BookingNotFoundException;
import com.vamigo.exceptions.CannotBookOwnRideException;
import com.vamigo.exceptions.ExternalRideNotBookableException;
import com.vamigo.exceptions.InsufficientSeatsException;
import com.vamigo.exceptions.InvalidBookingSegmentException;
import com.vamigo.exceptions.RideNotBookableException;
import com.vamigo.location.Location;
import com.vamigo.user.UserAccount;
import com.vamigo.vehicle.Vehicle;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@SuperBuilder
public class Ride extends AbstractTrip {

    @ManyToOne
    private UserAccount driver;

    private int totalSeats;

    @Column(name = "price_per_seat", precision = 6, scale = 2)
    private BigDecimal pricePerSeat;

    @ManyToOne
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "time_precision", nullable = false, length = 15)
    @Builder.Default
    private TimePrecision timePrecision = TimePrecision.EXACT;

    @Column(name = "departure_time")
    private Instant departureTime;

    @Column(name = "auto_approve", nullable = false)
    private boolean autoApprove;

    @Column(name = "door_to_door", nullable = false)
    private boolean doorToDoor;

    @Column(name = "accepts_packages", nullable = false)
    private boolean acceptsPackages;

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stopOrder ASC")
    private List<RideStop> stops;

    @OneToMany(mappedBy = "ride", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RideBooking> bookings;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "estimated_arrival_at")
    private Instant estimatedArrivalAt;

    public List<RideStop> getStops() {
        return stops == null ? List.of() : Collections.unmodifiableList(stops);
    }

    public List<RideBooking> getBookings() {
        return bookings == null ? List.of() : Collections.unmodifiableList(bookings);
    }

    public void replaceStops(List<RideStop> newStops) {
        if (this.stops == null) {
            this.stops = new ArrayList<>();
        } else {
            this.stops.clear();
        }
        if (newStops != null) {
            this.stops.addAll(newStops);
        }
    }

    public void replaceBookings(List<RideBooking> newBookings) {
        if (this.bookings == null) {
            this.bookings = new ArrayList<>();
        } else {
            this.bookings.clear();
        }
        if (newBookings != null) {
            this.bookings.addAll(newBookings);
        }
    }

    public void assignDriver(UserAccount driver) {
        this.driver = driver;
    }

    public void assignVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public void clearVehicle() {
        this.vehicle = null;
    }

    public void updateDetails(RideDetails details) {
        this.departureTime = details.departureTime();
        this.timePrecision = details.timePrecision();
        this.totalSeats = details.totalSeats();
        this.pricePerSeat = details.pricePerSeat();
        this.autoApprove = details.timePrecision() != TimePrecision.EXACT ? false : details.autoApprove();
        this.doorToDoor = details.doorToDoor();
        this.acceptsPackages = details.acceptsPackages();
        applyCommonDetails(details.description(), details.contactPhone(), details.currency());
    }

    public void recomputeArrival(Instant estimatedArrivalAt) {
        this.estimatedArrivalAt = estimatedArrivalAt;
    }

    public RideBooking addBooking(UserAccount passenger,
                                   Long boardStopOsmId,
                                   Long alightStopOsmId,
                                   int seatCount,
                                   BigDecimal proposedPrice,
                                   Instant now) {
        if (getSource() != RideSource.INTERNAL) {
            throw new ExternalRideNotBookableException(getId());
        }
        if (getStatus() != Status.ACTIVE) {
            throw new RideNotBookableException(getId(), getRideStatus().name());
        }
        if (driver != null && driver.getId().equals(passenger.getId())) {
            throw new CannotBookOwnRideException(getId());
        }

        RideStop boardStop = findStopByOsmId(boardStopOsmId);
        RideStop alightStop = findStopByOsmId(alightStopOsmId);
        if (boardStop.getStopOrder() >= alightStop.getStopOrder()) {
            throw new InvalidBookingSegmentException(getId(),
                    "Board stop must come before alight stop");
        }

        int available = getAvailableSeatsForSegment(
                boardStop.getStopOrder(), alightStop.getStopOrder());
        if (available < seatCount) {
            throw new InsufficientSeatsException(getId(), seatCount, available);
        }

        BookingStatus initialStatus = autoApprove
                ? BookingStatus.CONFIRMED
                : BookingStatus.PENDING;

        RideBooking booking = RideBooking.builder()
                .ride(this)
                .passenger(passenger)
                .boardStop(boardStop)
                .alightStop(alightStop)
                .status(initialStatus)
                .seatCount(seatCount)
                .proposedPrice(proposedPrice)
                .bookedAt(now)
                .resolvedAt(initialStatus == BookingStatus.CONFIRMED ? now : null)
                .build();

        if (this.bookings == null) {
            this.bookings = new ArrayList<>();
        }
        this.bookings.add(booking);
        touchLastModified(now);
        return booking;
    }

    public RideBooking confirmBooking(Long bookingId, Instant now) {
        RideBooking booking = findBookingById(bookingId);
        int available = getAvailableSeatsForSegment(
                booking.getBoardStop().getStopOrder(),
                booking.getAlightStop().getStopOrder());
        // Booking is still PENDING so its seats are already counted; add them back to compare.
        int availableExcludingSelf = available + booking.getSeatCount();
        if (availableExcludingSelf < booking.getSeatCount()) {
            throw new InsufficientSeatsException(getId(), booking.getSeatCount(), availableExcludingSelf);
        }
        booking.confirm(now);
        touchLastModified(now);
        return booking;
    }

    public RideBooking rejectBooking(Long bookingId, Instant now) {
        RideBooking booking = findBookingById(bookingId);
        booking.reject(now);
        touchLastModified(now);
        return booking;
    }

    public RideBooking cancelBookingByDriver(Long bookingId, String reason, Instant now) {
        RideBooking booking = findBookingById(bookingId);
        booking.cancelByDriver(reason, now);
        touchLastModified(now);
        return booking;
    }

    public RideBooking cancelBookingByPassenger(Long bookingId, String reason, Instant now) {
        RideBooking booking = findBookingById(bookingId);
        booking.cancelByPassenger(reason, now);
        touchLastModified(now);
        return booking;
    }

    private RideStop findStopByOsmId(Long osmId) {
        return getStops().stream()
                .filter(s -> s.getLocation().getOsmId().equals(osmId))
                .findFirst()
                .orElseThrow(() -> new InvalidBookingSegmentException(
                        getId(), "Stop with osmId " + osmId + " not found on this ride"));
    }

    private RideBooking findBookingById(Long bookingId) {
        if (bookings == null) {
            throw new BookingNotFoundException(getId(), bookingId);
        }
        return bookings.stream()
                .filter(b -> bookingId.equals(b.getId()))
                .findFirst()
                .orElseThrow(() -> new BookingNotFoundException(getId(), bookingId));
    }

    public void markCompleted(Instant now) {
        changeStatus(Status.COMPLETED);
        this.completedAt = now;
    }

    public void markExpired() {
        changeStatus(Status.EXPIRED);
    }

    public void cancel() {
        changeStatus(Status.CANCELLED);
    }

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
