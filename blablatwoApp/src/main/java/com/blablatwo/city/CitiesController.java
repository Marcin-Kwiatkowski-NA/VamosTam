package com.blablatwo.city;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Collection;
import java.util.Optional;

@RestController
public class CitiesController {
    private final CityAdminService cityAdminService;

    public CitiesController(CityAdminService cityAdminService) {
        this.cityAdminService = cityAdminService;
    }

    @GetMapping("/cities/{id}")
    public ResponseEntity<City> getCityById(@PathVariable long id) {
        Optional<City> cityOptional = cityAdminService.getById(id);
        return cityOptional.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/cities")
    public ResponseEntity<Collection<City>> getAllCities() {
        return ResponseEntity.ok(cityAdminService.getAllCities());
    }

    private URI getUriFromId(String id) {
        return ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}
