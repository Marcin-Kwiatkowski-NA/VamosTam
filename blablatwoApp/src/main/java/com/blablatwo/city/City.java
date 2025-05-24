package com.blablatwo.city;

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
public class City {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    private  String name;

    @Override
    public String toString() {
        return "City{id=%d, name='%s'}".formatted(id, name);
    }
}
