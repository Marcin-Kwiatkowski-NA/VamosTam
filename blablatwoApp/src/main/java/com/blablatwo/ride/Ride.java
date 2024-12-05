package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.vehicle.Vehicle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class Ride {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Traveler driver;

    @ManyToOne
    private City origin;

    @ManyToOne
    private City destination;

    private LocalDateTime departureTime;

    private int availableSeats;

    @Column(name = "price_per_seat", precision = 10, scale = 2)
    private BigDecimal pricePerSeat;

    @ManyToOne
    private Vehicle vehicle;

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
