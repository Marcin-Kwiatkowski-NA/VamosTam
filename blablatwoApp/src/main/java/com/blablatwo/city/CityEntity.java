package com.blablatwo.city;

import jakarta.persistence.*;

import java.util.Objects;

@Entity
@Table(name = "cities")
public class CityEntity {

    @Id
    @GeneratedValue
    @Column(name = "ID", updatable = false, nullable = false)
    private int id;

    @Column(name = "NAME")
    private  String name;

    public Integer getId() {
        return id;
    }

    public CityEntity setId(Integer id) {
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
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
