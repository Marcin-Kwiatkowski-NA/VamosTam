package com.blablatwo.ride;

import com.blablatwo.city.City;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.vehicle.Vehicle;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride {

    @Id
    @GeneratedValue
    private Long id;

    @ManyToOne
    private Traveler driver;

    @ManyToOne
    @NotNull
    private City origin;

    @ManyToOne
    @NotNull
    private City destination;

    @NotNull
    private LocalDateTime departureTime;

    private int availableSeats;

    @Column(name = "price_per_seat", precision = 10, scale = 2)
    private BigDecimal pricePerSeat;

    @ManyToOne
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    private RideStatus rideStatus; //     OPEN,    FULL,    COMPLETED,    CANCELLED

    @Column(name = "last_modified")
    private Instant lastModified;

    @Version
    int version;

    @ManyToMany
    private List<Traveler> passengers;
}