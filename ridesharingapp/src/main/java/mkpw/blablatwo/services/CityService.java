package mkpw.blablatwo.services;

import mkpw.blablatwo.model.City;

import java.util.Optional;

public interface CityService {
    Optional<City> findById(String id);
}
