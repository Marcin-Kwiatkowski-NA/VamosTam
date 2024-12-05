package com.blablatwo.city;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collection;

@RestController
public class CitiesController {
    private static final Logger LOGGER = LoggerFactory.getLogger(CitiesController.class);
    private final CityService rideService;

    public CitiesController(CityService rideService) {
        this.rideService = rideService;
    }

    @GetMapping("/cities/{id}")
    public ResponseEntity<CityDTO> getCityById(@PathVariable long id){
        return ResponseEntity.ok(rideService.getById(id));
    }

    @GetMapping("/cities")
    public Collection<CityDTO> getAllCities(){
        return rideService.getAllCities();
    }

    private URI getUriFromId(String id) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}
