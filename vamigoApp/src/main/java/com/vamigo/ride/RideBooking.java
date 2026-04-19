package com.vamigo.ride;

import com.vamigo.exceptions.InvalidBookingTransitionException;
import com.vamigo.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ride_booking", indexes = {
        @Index(name = "idx_ride_booking_ride_id", columnList = "ride_id"),
        @Index(name = "idx_ride_booking_passenger_id", columnList = "passenger_id"),
        @Index(name = "idx_ride_booking_status", columnList = "status")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
public class RideBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", nullable = false)
    private Ride ride;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "passenger_id", nullable = false)
    private UserAccount passenger;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "board_stop_id", nullable = false)
    private RideStop boardStop;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "alight_stop_id", nullable = false)
    private RideStop alightStop;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 25)
    private BookingStatus status;

    @Column(name = "seat_count", nullable = false)
    private int seatCount;

    @Column(name = "booked_at", nullable = false)
    private Instant bookedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "proposed_price", precision = 6, scale = 2)
    private BigDecimal proposedPrice;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    public boolean isActive() {
        return status.isActive();
    }

    public void confirm(Instant now) {
        transitionTo(BookingStatus.CONFIRMED);
        this.resolvedAt = now;
    }

    public void reject(Instant now) {
        transitionTo(BookingStatus.REJECTED);
        this.resolvedAt = now;
    }

    public void cancelByDriver(String reason, Instant now) {
        transitionTo(BookingStatus.CANCELLED_BY_DRIVER);
        applyCancellation(reason, now);
    }

    public void cancelByPassenger(String reason, Instant now) {
        transitionTo(BookingStatus.CANCELLED_BY_PASSENGER);
        applyCancellation(reason, now);
    }

    public void expire(Instant now) {
        transitionTo(BookingStatus.EXPIRED);
        this.resolvedAt = now;
    }

    private void transitionTo(BookingStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidBookingTransitionException(id, status, target);
        }
        this.status = target;
    }

    private void applyCancellation(String reason, Instant now) {
        this.resolvedAt = now;
        this.cancelledAt = now;
        if (reason != null && !reason.isBlank()) {
            this.cancellationReason = reason;
        }
    }
}
