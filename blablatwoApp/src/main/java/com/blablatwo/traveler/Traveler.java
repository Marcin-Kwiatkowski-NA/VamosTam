package com.blablatwo.traveler;

import com.blablatwo.ride.Ride;
import com.blablatwo.vehicle.Vehicle;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.List;

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

    private String password;  // Nullable for OAuth-only users

    private Integer enabled = 1;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Embedded
    private GoogleUser googleUser;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "name", length = 255)
    private String name;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Vehicle> vehicles;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Ride> ridesAsDriver;

    @ManyToMany(mappedBy = "passengers")
    private List<Ride> ridesAsPassenger;

    @Version
    int version;
}
