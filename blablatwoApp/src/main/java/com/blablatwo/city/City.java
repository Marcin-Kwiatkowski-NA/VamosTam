package com.blablatwo.city;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class City {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true, nullable = false)
    private Long osmId;

    @NotNull
    private String name;

    @Override
    public String toString() {
        return "City{id=%d, name='%s'}".formatted(id, name);
    }
}
