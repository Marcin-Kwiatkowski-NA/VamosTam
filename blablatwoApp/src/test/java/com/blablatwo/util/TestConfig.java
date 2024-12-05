package com.blablatwo.util;

import com.blablatwo.city.City;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.traveler.VehicleEntity;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static com.blablatwo.util.Constants.ID_100;
import static com.blablatwo.util.Constants.ID_ONE;

@TestConfiguration
 public class TestConfig {

    @Bean
    Traveler driver() {
        var driver = new Traveler();
        driver.setId(ID_100);
        return driver;
    }
    @Bean
    City origin() {
        var origin = new City();
        origin.setId(ID_ONE);
        return origin;
    }

    @Bean
    City destination() {
        var destination = new City();
        destination.setId(ID_100);
        return destination;
    }

    @Bean
    VehicleEntity vehicle() {
        var vehicle = new VehicleEntity();
        vehicle.setId(ID_100);
        return vehicle;
    }
}
