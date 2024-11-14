package com.blablatwo.traveler;

import com.blablatwo.city.CityEntity;
import com.blablatwo.ride.RideEntity;
import com.blablatwo.traveler.TravelerEntity;
import com.blablatwo.traveler.Vehicle;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "traveler")
public class TravelerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 255)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "enabled", nullable = false)
    private int enabled;

    String authority;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "last_name", length = 255)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "traveler_type", length = 10)
    private TravelerType travelerType; // Enum: DRIVER, PASSENGER, BOTH

    // Vehicles owned by the traveler

    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<VehicleEntity> vehicles;
    // Rides where the traveler is the driver

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RideEntity> ridesAsDriver;
    // Rides where the traveler is a passenger

    @ManyToMany(mappedBy = "passengers")
    private List<RideEntity> ridesAsPassenger;
    public Long getId() {
        return id;
    }

    public TravelerEntity setId(Long id) {
        this.id = id;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public TravelerEntity setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public TravelerEntity setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getAuthority() {
        return authority;
    }

    public TravelerEntity setAuthority(String authority) {
        this.authority = authority;
        return this;
    }

    public int getEnabled() {
        return enabled;
    }

    public TravelerEntity setEnabled(int enabled) {
        this.enabled = enabled;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public TravelerEntity setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public TravelerEntity setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
        return this;
    }

    public String getFirstName() {
        return firstName;
    }

    public TravelerEntity setFirstName(String firstName) {
        this.firstName = firstName;
        return this;
    }

    public String getLastName() {
        return lastName;
    }

    public TravelerEntity setLastName(String lastName) {
        this.lastName = lastName;
        return this;
    }

    public TravelerType getTravelerType() {
        return travelerType;
    }

    public TravelerEntity setTravelerType(TravelerType travelerType) {
        this.travelerType = travelerType;
        return this;
    }

    public List<VehicleEntity> getVehicles() {
        return vehicles;
    }

    public TravelerEntity setVehicles(List<VehicleEntity> vehicles) {
        this.vehicles = vehicles;
        return this;
    }

    public List<RideEntity> getRidesAsDriver() {
        return ridesAsDriver;
    }

    public TravelerEntity setRidesAsDriver(List<RideEntity> ridesAsDriver) {
        this.ridesAsDriver = ridesAsDriver;
        return this;
    }

    public List<RideEntity> getRidesAsPassenger() {
        return ridesAsPassenger;
    }

    public TravelerEntity setRidesAsPassenger(List<RideEntity> ridesAsPassenger) {
        this.ridesAsPassenger = ridesAsPassenger;
        return this;
    }
}
