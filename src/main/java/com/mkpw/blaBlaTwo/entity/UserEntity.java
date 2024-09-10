package com.mkpw.blaBlaTwo.entity;

import com.mkpw.blaBlaTwo.model.Ride;
import com.mkpw.blaBlaTwo.model.User.RoleEnum;
import jakarta.persistence.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table (name = "users")
public class UserEntity {

    @Id
    @GeneratedValue
    @Column(name = "ID", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "EMAIL")
    private String email;

    @Column(name = "PHONE")
    private String phone;

    @OneToMany(mappedBy = "driver", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RideEntity> rides;

    public UUID getId() {
        return id;
    }

    public UserEntity setId(UUID id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public UserEntity setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserEntity setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getPhone() {
        return phone;
    }

    public UserEntity setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", email=" + email +
                ", phone='" + phone + '\'' +
                '}';
    }
}
