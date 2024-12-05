package com.blablatwo;

import com.blablatwo.city.City;
import com.blablatwo.city.CityRepository;
import com.blablatwo.traveler.Traveler;
import com.blablatwo.traveler.VehicleEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static com.blablatwo.config.Roles.ROLE_DRIVER;
import static com.blablatwo.traveler.TravelerType.DRIVER;
import static com.blablatwo.util.Constants.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class RepositoryTest {

    @Autowired
    private CityRepository cityRepository;

    protected Traveler driver;
    protected City origin;
    protected City destination;
    protected VehicleEntity vehicle;

    public RepositoryTest() {
        this.vehicle = new VehicleEntity();
        this.origin = City.builder()
                .name(CITY_NAME_ORIGIN)
                .build();
        this.destination = City.builder()
                .name(CITY_NAME_DESTINATION)
                .build();
        this.driver = Traveler.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .enabled(ENABLED)
                .authority(ROLE_DRIVER)
                .email(EMAIL)
                .phoneNumber(TELEPHONE)
                .name(CRISTIANO)
                .type(DRIVER)
                .vehicles(List.of(vehicle))
                .build();
    }
    @BeforeAll
    void setUp() {
        cityRepository.save(origin);
        cityRepository.save(destination);
    }
}
