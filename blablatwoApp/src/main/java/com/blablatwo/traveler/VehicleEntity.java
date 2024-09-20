package com.blablatwo.traveler;

import com.blablatwo.ride.RideEntity;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "vehicle")
public class VehicleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "make", length = 255)
    private String make;

    @Column(name = "model", length = 255)
    private String model;

    @Column(name = "production_year")
    private Integer productionYear;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "license_plate", length = 50)
    private String licensePlate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private TravelerEntity owner;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RideEntity> rides;

    public Long getId() {
        return id;
    }

    public VehicleEntity setId(Long id) {
        this.id = id;
        return this;
    }

    public String getMake() {
        return make;
    }

    public VehicleEntity setMake(String make) {
        this.make = make;
        return this;
    }

    public String getModel() {
        return model;
    }

    public VehicleEntity setModel(String model) {
        this.model = model;
        return this;
    }

    public Integer getProductionYear() {
        return productionYear;
    }

    public VehicleEntity setProductionYear(Integer productionYear) {
        this.productionYear = productionYear;
        return this;
    }

    public String getColor() {
        return color;
    }

    public VehicleEntity setColor(String color) {
        this.color = color;
        return this;
    }

    public String getLicensePlate() {
        return licensePlate;
    }

    public VehicleEntity setLicensePlate(String licensePlate) {
        this.licensePlate = licensePlate;
        return this;
    }

    public TravelerEntity getOwner() {
        return owner;
    }

    public VehicleEntity setOwner(TravelerEntity owner) {
        this.owner = owner;
        return this;
    }

    public List<RideEntity> getRides() {
        return rides;
    }

    public VehicleEntity setRides(List<RideEntity> rides) {
        this.rides = rides;
        return this;
    }
}