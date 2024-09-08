package com.mkpw.blaBlaTwo.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "cities")
public class CityEntity {

    @Id
    @GeneratedValue
    @Column(name = "ID", updatable = false, nullable = false)
    private UUID id; // Integer? get rid of it?

    @Column(name = "NAME")
    private String name;

    public UUID getId() {
        return id;
    }

    public CityEntity setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public CityEntity setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public String toString() {
        return "CityEntity{" +
                "name='" + name + '\'' +
                ", id=" + id +
                '}';
    }
}
