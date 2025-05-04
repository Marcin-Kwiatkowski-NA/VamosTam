package com.blablatwo.city;

import java.util.Collection;

public interface CityService {
    CityDTO getById(Long id);

    Collection<CityDTO> getAllCities();
}
