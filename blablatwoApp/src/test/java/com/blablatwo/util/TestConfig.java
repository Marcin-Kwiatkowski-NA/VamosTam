package com.blablatwo.util;

import com.blablatwo.city.CityEntity;
import com.blablatwo.traveler.TravelerEntity;
import com.blablatwo.traveler.VehicleEntity;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static com.blablatwo.util.Constants.*;

@TestConfiguration
 public class TestConfig {

    @Bean
    TravelerEntity driver() {
        var driver = new TravelerEntity();
        driver.setId(ID_100_L);
        return driver;
    }
    @Bean
    CityEntity origin() {
        var origin = new CityEntity();
        origin.setId(ONE);
        return origin;
    }

    @Bean
    CityEntity destination() {
        var destination = new CityEntity();
        destination.setId(ID_100_INT);
        return destination;
    }

    @Bean
    VehicleEntity vehicle() {
        var vehicle = new VehicleEntity();
        vehicle.setId(ID_100_L);
        return vehicle;
    }
}
