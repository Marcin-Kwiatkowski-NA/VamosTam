package mkpw.blablatwo.utils;

import mkpw.blablatwo.entity.CityEntity;
import mkpw.blablatwo.entity.RideEntity;
import mkpw.blablatwo.entity.UserEntity;
import mkpw.blablatwo.model.City;
import mkpw.blablatwo.model.Ride;
import mkpw.blablatwo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.util.UUID;

import static mkpw.blablatwo.utils.ConstantsTest.*;

@TestConfiguration
public class TestBeans {

    @Bean
    City CreateCity() {
        City city = new City();
        city.id(UUID.randomUUID())
                .name("Warszawa");
        return city;
    }

    @Bean
    CityEntity CreateCityEntity() {
        CityEntity cityEntity = new CityEntity();
        cityEntity.setId(UUID.randomUUID())
                .setName("Warszawa");
        return cityEntity;
    }

    @Bean
    Ride CreateRide() {
        Ride ride = new Ride();
        City city = new City();
        User user = new User();
        ride.id(UUID.fromString(RIDE_ID))
                .destinationCity(city)
                .startCity(city)
                .departureTime(DATE_TIME)
                .driver(user)
                .price(BIG_DECIMAL)
                .petFriendly(Boolean.TRUE)
                .status(Ride.StatusEnum.ACTIVE);
        return ride;
    }

    @Bean
    RideEntity CreateRideEntity() {
        RideEntity rideEntity = new RideEntity();
        CityEntity city = new CityEntity();
        UserEntity user = new UserEntity();
        rideEntity.setId(UUID.fromString(RIDE_ID))
                .setDestinationCity(city)
                .setStartCity(city)
                .setDepartureTime(TIMESTAMP)
                .setDriver(user)
                .setPrice(BIG_DECIMAL)
                .setPetFriendly(Boolean.TRUE)
                .setStatus(Ride.StatusEnum.ACTIVE);
        return rideEntity;
    }

    @Bean
    User CreateCUser() {
        User user = new User();
        user.id(UUID.randomUUID())
                .email(EMAIL)
                .phone(TELEPHONE)
                .name(CRISTIANO)
                .role(User.RoleEnum.DRIVER);
        return user;
    }

    @Bean
    UserEntity CreateCUserEntity() {
        UserEntity userEntity = new UserEntity();
        userEntity.setId(UUID.randomUUID())
                .setEmail(EMAIL)
                .setPhone(TELEPHONE)
                .setName(CRISTIANO)
                .setRole(User.RoleEnum.DRIVER);
        return userEntity;
    }

}
