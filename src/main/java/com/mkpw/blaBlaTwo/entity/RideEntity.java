package com.mkpw.blaBlaTwo.entity;

import com.mkpw.blaBlaTwo.model.Ride.StatusEnum;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(name = "rides")
public class RideEntity {
    @Id
    @GeneratedValue
    @Column (name = "ID", updatable = false, nullable = false)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "DRIVER_ID", referencedColumnName = "ID")
    private UserEntity driver;

    @Column(name = "price")
    private BigDecimal price;

    @Column(name = "pet_friendly")
    private boolean petFriendly;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private StatusEnum status;

    @Column(name = "departure_time")
    private Timestamp departureTime;

    public UUID getId() {
        return id;
    }

    public RideEntity setId(UUID id) {
        this.id = id;
        return this;
    }

    public UserEntity getDriver() {
        return driver;
    }

    public RideEntity setDriver(UserEntity driver) {
        this.driver = driver;
        return this;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public RideEntity setPrice(BigDecimal price) {
        this.price = price;
        return this;
    }

    public boolean isPetFriendly() {
        return petFriendly;
    }

    public RideEntity setPetFriendly(boolean petFriendly) {
        this.petFriendly = petFriendly;
        return this;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public RideEntity setStatus(StatusEnum status) {
        this.status = status;
        return this;
    }

    public Timestamp getDepartureTime() {
        return departureTime;
    }

    public RideEntity setDepartureTime(Timestamp departureTime) {
        this.departureTime = departureTime;
        return this;
    }
}
