package com.mkpw.blaBlaTwo.entity;

import jakarta.persistence.*;

import java.util.UUID;

//@Entity
//@Table(name = "rides")
public class RideEntity {
    @Id
    @GeneratedValue
    @Column (name = "ID", updatable = false, nullable = false)
    private UUID id;
    @OneToOne
    @JoinColumn(name = "driver_id", referencedColumnName = "id")
    private UserEntity user;
}
