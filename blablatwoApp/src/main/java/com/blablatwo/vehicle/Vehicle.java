package com.blablatwo.vehicle;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    private String make;

    @NotNull
    private String model;

    private Integer productionYear;

    private String color;

    private String licensePlate;
}