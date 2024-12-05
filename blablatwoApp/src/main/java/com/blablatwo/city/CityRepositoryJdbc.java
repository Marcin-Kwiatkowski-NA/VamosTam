package com.blablatwo.city;

import java.util.List;
import java.util.Optional;

public interface CityRepositoryJdbc {
    Optional<City> findById(Long id);

    List<City> findAll();

    City save(City city);

    boolean deleteById(Long id);

    Optional<City> update(City city);
}
