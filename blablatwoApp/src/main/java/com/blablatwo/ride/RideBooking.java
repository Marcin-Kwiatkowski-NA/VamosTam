package com.blablatwo.ride;

import com.blablatwo.user.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "ride_booking", uniqueConstraints = {
        @UniqueConstraint(name = "uk_ride_booking_ride_passenger",
                columnNames = {"ride_id", "passenger_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RideBooking {

    @Id
    @GeneratedValue
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

    @Column(name = "booked_at", nullable = false)
    private Instant bookedAt;
}
