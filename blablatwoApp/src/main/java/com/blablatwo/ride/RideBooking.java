package com.blablatwo.ride;

import com.blablatwo.user.UserAccount;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ride_booking", indexes = {
        @Index(name = "idx_ride_booking_ride_id", columnList = "ride_id"),
        @Index(name = "idx_ride_booking_passenger_id", columnList = "passenger_id"),
        @Index(name = "idx_ride_booking_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @Column(name = "proposed_price", precision = 10, scale = 2)
    private BigDecimal proposedPrice;

    public boolean isActive() {
        return status.isActive();
    }
}
