package com.blablatwo.city;

import java.util.List;
import java.util.Optional;

public interface CityRepository {
    Optional<CityEntity> findById(long id);

    List<CityEntity> findAll();

    CityEntity save(CityEntity city);

    boolean deleteById(long id);

    Optional<CityEntity> update(CityEntity city);
}
