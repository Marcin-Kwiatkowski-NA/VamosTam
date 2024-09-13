package mkpw.blablatwo.services;

import mkpw.blablatwo.model.City;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CityServiceImpl implements CityService{

    @Override
    public Optional<City> findById(String id) {
        return Optional.empty();
    }
}
