package com.blablatwo.city;

import java.util.List;
import java.util.Optional;

public interface CityRepository {
    Optional<CityEntity> findById(int id);

    List<CityEntity> findAll();

    CityEntity save(CityEntity city);

    boolean deleteById(int id);

    Optional<CityEntity> update(CityEntity city);
}
