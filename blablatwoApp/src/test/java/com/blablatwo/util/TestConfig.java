package com.blablatwo.util;

import com.blablatwo.city.CityEntity;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.RideEntity;
import com.blablatwo.traveler.TravelerEntity;
import com.blablatwo.traveler.VehicleEntity;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.List;

import static com.blablatwo.util.Constants.*;

@TestConfiguration
 public class TestConfig {

    @Bean
    TravelerEntity driver() {
        var driver = new TravelerEntity();
        driver.setId(ID_100);
        return driver;
    }
    @Bean
    CityEntity origin() {
        var origin = new CityEntity();
        origin.setId(ID_ONE);
        return origin;
    }

    @Bean
    CityEntity destination() {
        var destination = new CityEntity();
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
