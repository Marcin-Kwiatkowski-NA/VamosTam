package com.blablatwo.city;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CityService {
    Optional<City> getById(Long id);
    Optional<City> findByName(String name);
    List<City> getAllCities();
    CityResponseDto create(CityCreationDto cityDto);
    CityResponseDto update(CityCreationDto cityDto, Long id);
    void delete(Long id);
}