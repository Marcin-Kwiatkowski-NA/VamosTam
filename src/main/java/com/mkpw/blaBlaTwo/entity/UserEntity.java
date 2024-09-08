package com.mkpw.blaBlaTwo.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table (name = "users")
public class UserEntity {

    @Id
    @GeneratedValue
    @Column(name = "ID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name")
    private String name;
}
