package mkpw.blablatwo.controllers;

import mkpw.blablatwo.api.CityApi;
import mkpw.blablatwo.model.City;
import mkpw.blablatwo.services.CityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CityController implements CityApi {

    public static final Logger LOGGER = LoggerFactory.getLogger(CityController.class);

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @Override
    public ResponseEntity<City> getCityById(String id) {
        return cityService.findById(id)
                .map(city -> {
                    return ResponseEntity
                            .ok()
//                            .eTag(Integer.toString(city.getVersion()))
                            .body(city);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
