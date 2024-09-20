package com.blablatwo.ride;

import com.blablatwo.city.CityEntity;
import com.blablatwo.traveler.TravelerEntity;
import com.blablatwo.traveler.Vehicle;
import com.blablatwo.traveler.VehicleEntity;
import jakarta.persistence.*;
import org.springframework.data.annotation.LastModifiedDate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ride")
public class RideEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Driver of the ride
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private TravelerEntity driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_id", nullable = false)
    private CityEntity origin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_id", nullable = false)
    private CityEntity destination;

    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    @Column(name = "price_per_seat", precision = 10, scale = 2)
    private BigDecimal pricePerSeat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id")
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
    private List<TravelerEntity> passengers;

    public Long getId() {
        return id;
    }

    public RideEntity setId(Long id) {
        this.id = id;
        return this;
    }

    public TravelerEntity getDriver() {
        return driver;
    }

    public RideEntity setDriver(TravelerEntity driver) {
        this.driver = driver;
        return this;
    }

    public CityEntity getOrigin() {
        return origin;
    }

    public RideEntity setOrigin(CityEntity origin) {
        this.origin = origin;
        return this;
    }

    public CityEntity getDestination() {
        return destination;
    }

    public RideEntity setDestination(CityEntity destination) {
        this.destination = destination;
        return this;
    }

    public LocalDateTime getDepartureTime() {
        return departureTime;
    }

    public RideEntity setDepartureTime(LocalDateTime departureTime) {
        this.departureTime = departureTime;
        return this;
    }

    public int getAvailableSeats() {
        return availableSeats;
    }

    public RideEntity setAvailableSeats(int availableSeats) {
        this.availableSeats = availableSeats;
        return this;
    }

    public BigDecimal getPricePerSeat() {
        return pricePerSeat;
    }

    public RideEntity setPricePerSeat(BigDecimal pricePerSeat) {
        this.pricePerSeat = pricePerSeat;
        return this;
    }

    public VehicleEntity getVehicle() {
        return vehicle;
    }

    public RideEntity setVehicle(VehicleEntity vehicle) {
        this.vehicle = vehicle;
        return this;
    }

    public RideStatus getRideStatus() {
        return rideStatus;
    }

    public RideEntity setRideStatus(RideStatus rideStatus) {
        this.rideStatus = rideStatus;
        return this;
    }

    public List<TravelerEntity> getPassengers() {
        return passengers;
    }

    public RideEntity setPassengers(List<TravelerEntity> passengers) {
        this.passengers = passengers;
        return this;
    }

    public Instant getLastModified() {
        return lastModified;
    }

    public RideEntity setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
        return this;
    }
// Fields 'stops'  not included as they require additional schema definitions.

}
