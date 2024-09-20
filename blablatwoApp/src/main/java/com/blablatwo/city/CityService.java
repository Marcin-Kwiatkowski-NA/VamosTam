package com.blablatwo.city;

import java.util.Collection;

public interface CityService {
    CityDTO getById(int id);

    Collection<CityDTO> getAllCities();
}
