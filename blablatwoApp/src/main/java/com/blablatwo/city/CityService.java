package com.blablatwo.city;

import java.util.Collection;

public interface CityService {
    CityDTO getById(long id);

    Collection<CityDTO> getAllCities();
}
