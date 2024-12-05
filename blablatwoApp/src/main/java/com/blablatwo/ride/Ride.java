package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.traveler.VehicleEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ride")
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Driver of the ride
    @ManyToOne(fetch = FetchType.LAZY)
    private Traveler driver;

    @ManyToOne(fetch = FetchType.LAZY)
    private City origin;

    @ManyToOne(fetch = FetchType.LAZY)
    private City destination;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    @Column(name = "price_per_seat", precision = 10, scale = 2)
    private BigDecimal pricePerSeat;

    @ManyToOne(fetch = FetchType.LAZY)
    private VehicleEntity vehicle;

    @Enumerated(EnumType.STRING)
    @Column(name = "ride_status", length = 10)
    private RideStatus rideStatus;

    @Column(name = "last_modified")
    private Instant lastModified;

    // Passengers
    @ManyToMany
    @JoinTable(name = "ride_passengers",
            joinColumns = @JoinColumn(name = "ride_id"),
            inverseJoinColumns = @JoinColumn(name = "traveler_id"))
    private List<Traveler> passengers;
}
