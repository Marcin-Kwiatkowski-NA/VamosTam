package com.vamigo.vehicle;

import com.vamigo.user.UserAccount;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PACKAGE)
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserAccount owner;

    @NotNull
    private String make;

    @NotNull
    private String model;

    private Integer productionYear;

    @Enumerated(EnumType.STRING)
    private VehicleColor color;

    private String licensePlate;

    private String description;

    private String photoObjectKey;

    public void assignOwner(UserAccount owner) {
        this.owner = owner;
    }

    public void updateDetails(VehicleDetails details) {
        this.make = details.make();
        this.model = details.model();
        this.productionYear = details.productionYear();
        this.color = details.color();
        this.licensePlate = details.licensePlate();
        this.description = details.description();
    }

    public void updatePhoto(String objectKey) {
        this.photoObjectKey = objectKey;
    }

    public void clearPhoto() {
        this.photoObjectKey = null;
    }
}
