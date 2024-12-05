package com.blablatwo.traveler;

import com.blablatwo.config.Roles;
import com.blablatwo.ride.Ride;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

import static com.blablatwo.config.Roles.ROLE_PASSENGER;
import static com.blablatwo.traveler.TravelerType.PASSENGER;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Traveler {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "enabled", nullable = false)
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

    // Vehicles owned by the traveler

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VehicleEntity> vehicles;
    // Rides where the traveler is the driver

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ride> ridesAsDriver;
    // Rides where the traveler is a passenger

    @ManyToMany(mappedBy = "passengers")
    private List<Ride> ridesAsPassenger;
}
