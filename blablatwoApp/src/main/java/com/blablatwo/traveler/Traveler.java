package com.blablatwo.traveler;

import com.blablatwo.config.Roles;
import com.blablatwo.ride.Ride;
import com.blablatwo.vehicle.Vehicle;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.blablatwo.config.Roles.ROLE_PASSENGER;
import static com.blablatwo.traveler.TravelerType.PASSENGER;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Component
public class Traveler {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @NotNull
    private String password;

    @NotNull
    private int enabled = 1;

    @Column(name = "authority", nullable = false)
    @Enumerated(EnumType.STRING)
    Roles authority = ROLE_PASSENGER;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "name", length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 10)
    private TravelerType type = PASSENGER; // Enum: DRIVER, PASSENGER, BOTH

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Vehicle> vehicles;
    // Rides where the traveler is the driver

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ride> ridesAsDriver;
    // Rides where the traveler is a passenger

    @ManyToMany(mappedBy = "passengers")
    private List<Ride> ridesAsPassenger;
}
