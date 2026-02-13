package com.blablatwo.util;

import com.blablatwo.city.City;
import com.blablatwo.city.CityDto;
import com.blablatwo.city.Lang;
import com.blablatwo.dto.ContactMethodDto;
import com.blablatwo.dto.ContactType;
import com.blablatwo.dto.UserCardDto;
import com.blablatwo.ride.Ride;
import com.blablatwo.ride.RideSource;
import com.blablatwo.ride.RideStatus;
import com.blablatwo.ride.RideStop;
import com.blablatwo.ride.dto.BookRideRequest;
import com.blablatwo.ride.dto.ExternalRideCreationDto;
import com.blablatwo.ride.dto.RideCreationDto;
import com.blablatwo.ride.dto.RideResponseDto;
import com.blablatwo.seat.SeatStatus;
import com.blablatwo.seat.dto.ExternalSeatCreationDto;
import com.blablatwo.seat.dto.SeatResponseDto;
import com.blablatwo.user.AccountStatus;
import com.blablatwo.user.Role;
import com.blablatwo.user.UserAccount;
import com.blablatwo.user.UserProfile;
import com.blablatwo.user.UserStats;
import com.blablatwo.vehicle.Vehicle;
import com.blablatwo.vehicle.VehicleCreationDto;
import com.blablatwo.vehicle.VehicleResponseDto;

import java.util.List;
import java.util.Set;

import static com.blablatwo.util.Constants.*;

/**
 * Object Mother for test fixtures. Provides pre-configured builders and
 * factory methods for domain objects used across test classes.
 * <p>
 * Builder-based types return a pre-filled builder — callers override fields then call .build().
 * Small records (CityDto, ContactMethodDto) return completed instances.
 */
public final class TestFixtures {

    private TestFixtures() {}

    // ──────────────── City ────────────────

    public static City.CityBuilder anOriginCity() {
        return City.builder()
                .id(ID_ONE)
                .placeId(ID_ONE)
                .namePl(CITY_NAME_ORIGIN)
                .normNamePl(CITY_NAME_ORIGIN.toLowerCase())
                .countryCode("PL")
                .population(100_000L);
    }

    public static City.CityBuilder aDestinationCity() {
        return City.builder()
                .id(2L)
                .placeId(2L)
                .namePl(CITY_NAME_DESTINATION)
                .normNamePl(CITY_NAME_DESTINATION.toLowerCase())
                .countryCode("PL")
                .population(200_000L);
    }

    public static City.CityBuilder aKrakowCity() {
        return City.builder()
                .placeId(PLACE_ID_KRAKOW)
                .namePl(CITY_NAME_KRAKOW)
                .normNamePl("krakow")
                .countryCode("PL")
                .population(POPULATION_KRAKOW);
    }

    public static City.CityBuilder aWarsawCity() {
        return City.builder()
                .placeId(PLACE_ID_WARSAW)
                .namePl(CITY_NAME_WARSAW)
                .normNamePl(CITY_NAME_WARSAW.toLowerCase())
                .countryCode("PL")
                .population(POPULATION_WARSAW);
    }

    public static CityDto krakowCityDto() {
        return new CityDto(PLACE_ID_KRAKOW, CITY_NAME_KRAKOW, "PL", POPULATION_KRAKOW);
    }

    public static CityDto warsawCityDto() {
        return new CityDto(PLACE_ID_WARSAW, CITY_NAME_WARSAW, "PL", POPULATION_WARSAW);
    }

    public static CityDto originCityDto() {
        return new CityDto(ID_ONE, CITY_NAME_ORIGIN, "PL", 100_000L);
    }

    public static CityDto destinationCityDto() {
        return new CityDto(2L, CITY_NAME_DESTINATION, "PL", 200_000L);
    }

    public static CityDto parisCityDto() {
        return new CityDto(1L, CITY_NAME_PARIS, "FR", 2_000_000L);
    }

    public static CityDto lyonCityDto() {
        return new CityDto(2L, CITY_NAME_LYON, "FR", 500_000L);
    }

    // ──────────────── User ────────────────

    public static UserAccount.UserAccountBuilder aDriverAccount() {
        return UserAccount.builder()
                .id(ID_ONE)
                .email(TRAVELER_EMAIL_USER1);
    }

    public static UserAccount.UserAccountBuilder aPassengerAccount() {
        return UserAccount.builder()
                .id(2L)
                .email(TRAVELER_EMAIL_USER2);
    }

    public static UserAccount.UserAccountBuilder anActiveUserAccount() {
        return UserAccount.builder()
                .email(EMAIL)
                .passwordHash(PASSWORD)
                .status(AccountStatus.ACTIVE)
                .roles(Set.of(Role.USER));
    }

    public static UserProfile.UserProfileBuilder aUserProfile(UserAccount account) {
        return UserProfile.builder()
                .account(account)
                .displayName(CRISTIANO)
                .phoneNumber(TELEPHONE)
                .stats(new UserStats());
    }

    public static UserCardDto.UserCardDtoBuilder aDriverCard() {
        return UserCardDto.builder()
                .id(ID_ONE)
                .name(CRISTIANO)
                .vehicles(List.of());
    }

    // ──────────────── Vehicle ────────────────

    public static Vehicle.VehicleBuilder aTesla() {
        return Vehicle.builder()
                .id(ID_ONE)
                .make(VEHICLE_MAKE_TESLA)
                .model(VEHICLE_MODEL_MODEL_S)
                .productionYear(VEHICLE_PRODUCTION_YEAR_2021)
                .color(VEHICLE_COLOR_RED)
                .licensePlate(VEHICLE_LICENSE_PLATE_1);
    }

    public static Vehicle.VehicleBuilder aBmw() {
        return Vehicle.builder()
                .make(VEHICLE_MAKE_BMW)
                .model(VEHICLE_MODEL_X5)
                .productionYear(VEHICLE_PRODUCTION_YEAR_2020)
                .color(VEHICLE_COLOR_BLACK)
                .licensePlate(VEHICLE_LICENSE_PLATE_2);
    }

    public static VehicleCreationDto.VehicleCreationDtoBuilder aVehicleCreation() {
        return VehicleCreationDto.builder()
                .make(VEHICLE_MAKE_TESLA)
                .model(VEHICLE_MODEL_MODEL_S)
                .productionYear(VEHICLE_PRODUCTION_YEAR_2021)
                .color(VEHICLE_COLOR_RED)
                .licensePlate(VEHICLE_LICENSE_PLATE_1);
    }

    public static VehicleResponseDto.VehicleResponseDtoBuilder aTeslaVehicleResponse() {
        return VehicleResponseDto.builder()
                .id(ID_ONE)
                .make(VEHICLE_MAKE_TESLA)
                .model(VEHICLE_MODEL_MODEL_S)
                .productionYear(VEHICLE_PRODUCTION_YEAR_2021)
                .color(VEHICLE_COLOR_RED)
                .licensePlate(VEHICLE_LICENSE_PLATE_1);
    }

    // ──────────────── Contact ────────────────

    public static ContactMethodDto phoneContact() {
        return new ContactMethodDto(ContactType.PHONE, TELEPHONE);
    }

    public static ContactMethodDto facebookContact(String url) {
        return new ContactMethodDto(ContactType.FACEBOOK_LINK, url);
    }

    // ──────────────── Ride ────────────────

    public static RideCreationDto.RideCreationDtoBuilder aRideCreationDto() {
        return RideCreationDto.builder()
                .originPlaceId(ID_ONE)
                .destinationPlaceId(2L)
                .departureTime(LOCAL_DATE_TIME)
                .isApproximate(false)
                .availableSeats(ONE)
                .pricePerSeat(BIG_DECIMAL)
                .vehicleId(ID_ONE)
                .description(RIDE_DESCRIPTION);
    }

    public static Ride.RideBuilder<?, ?> aRide(City origin, City destination) {
        Ride.RideBuilder<?, ?> builder = Ride.builder()
                .id(ID_100)
                .driver(aDriverAccount().build())
                .departureDate(LOCAL_DATE)
                .departureTime(LOCAL_TIME)
                .isApproximate(false)
                .totalSeats(ONE)
                .pricePerSeat(BIG_DECIMAL)
                .vehicle(aTesla().build())
                .lastModified(INSTANT)
                .description(RIDE_DESCRIPTION);

        // Build a temporary ride to set up stops with back-references
        // Callers who need stops should call .build() and then set stops
        return builder;
    }

    public static List<RideStop> buildStops(Ride ride, City origin, City destination) {
        return List.of(
                RideStop.builder().ride(ride).city(origin).stopOrder(0)
                        .departureTime(LOCAL_DATE.atTime(LOCAL_TIME)).build(),
                RideStop.builder().ride(ride).city(destination).stopOrder(1)
                        .departureTime(null).build()
        );
    }

    public static Ride buildRideWithStops(City origin, City destination) {
        Ride ride = aRide(origin, destination).build();
        ride.setStops(new java.util.ArrayList<>(buildStops(ride, origin, destination)));
        return ride;
    }

    public static Ride buildRideWithStops() {
        return buildRideWithStops(anOriginCity().build(), aDestinationCity().build());
    }

    public static Ride.RideBuilder<?, ?> aRide() {
        return aRide(anOriginCity().build(), aDestinationCity().build());
    }

    public static BookRideRequest.BookRideRequestBuilder aBookRideRequest() {
        return BookRideRequest.builder()
                .boardStopPlaceId(ID_ONE)
                .alightStopPlaceId(2L);
    }

    public static RideResponseDto.RideResponseDtoBuilder aRideResponseDto() {
        return RideResponseDto.builder()
                .id(ID_100)
                .source(RideSource.INTERNAL)
                .origin(originCityDto())
                .destination(destinationCityDto())
                .stops(List.of())
                .departureTime(LOCAL_DATE_TIME)
                .isApproximate(false)
                .pricePerSeat(BIG_DECIMAL)
                .availableSeats(ONE)
                .seatsTaken(0)
                .totalSeats(ONE)
                .description(RIDE_DESCRIPTION)
                .driver(aDriverCard().build())
                .contactMethods(List.of(phoneContact()))
                .vehicle(aTeslaVehicleResponse().build())
                .rideStatus(RideStatus.OPEN);
    }

    // ── French Ride Scenario ──

    public static RideResponseDto.RideResponseDtoBuilder aFrenchRideResponse() {
        return aRideResponseDto()
                .source(RideSource.FACEBOOK)
                .origin(parisCityDto())
                .destination(lyonCityDto())
                .pricePerSeat(PRICE_25)
                .availableSeats(3)
                .description(FRENCH_RIDE_DESCRIPTION)
                .driver(aDriverCard().id(9999L).name(JEAN_DUPONT).build())
                .contactMethods(List.of(facebookContact(FACEBOOK_GROUP_URL)))
                .vehicle(null);
    }

    // ──────────────── External ────────────────

    public static ExternalRideCreationDto.ExternalRideCreationDtoBuilder anExternalRideCreationDto() {
        return ExternalRideCreationDto.builder()
                .originCityName(CITY_NAME_ORIGIN)
                .destinationCityName(CITY_NAME_DESTINATION)
                .lang(Lang.PL)
                .departureDate(LOCAL_DATE)
                .departureTime(LOCAL_TIME)
                .isApproximate(false)
                .availableSeats(ONE)
                .pricePerSeat(BIG_DECIMAL)
                .description(RIDE_DESCRIPTION)
                .externalId("fb-12345")
                .rawContent("raw content")
                .phoneNumber(TELEPHONE)
                .authorName(CRISTIANO)
                .sourceUrl("https://facebook.com/post/12345");
    }

    public static ExternalRideCreationDto.ExternalRideCreationDtoBuilder anExternalRideWithStops() {
        return anExternalRideCreationDto()
                .intermediateStopCityNames(List.of(CITY_NAME_KRAKOW));
    }

    // ── French External Ride Scenario ──

    public static ExternalRideCreationDto.ExternalRideCreationDtoBuilder aFrenchRideCreation() {
        return anExternalRideCreationDto()
                .originCityName(CITY_NAME_PARIS)
                .destinationCityName(CITY_NAME_LYON)
                .lang(Lang.EN)
                .availableSeats(3)
                .pricePerSeat(PRICE_25)
                .description(FRENCH_RIDE_DESCRIPTION)
                .phoneNumber(TELEPHONE_FR)
                .authorName(JEAN_DUPONT)
                .sourceUrl(FACEBOOK_GROUP_URL);
    }

    public static ExternalSeatCreationDto.ExternalSeatCreationDtoBuilder anExternalSeatCreationDto() {
        return ExternalSeatCreationDto.builder()
                .originCityName(CITY_NAME_ORIGIN)
                .destinationCityName(CITY_NAME_DESTINATION)
                .lang(Lang.PL)
                .departureDate(LOCAL_DATE)
                .departureTime(LOCAL_TIME)
                .isApproximate(false)
                .count(ONE)
                .priceWillingToPay(BIG_DECIMAL)
                .description("Looking for a ride")
                .externalId("fb-seat-12345")
                .rawContent("raw content")
                .phoneNumber(TELEPHONE)
                .authorName(CRISTIANO)
                .sourceUrl("https://facebook.com/post/12345");
    }

    // ──────────────── Seat ────────────────

    public static SeatResponseDto.SeatResponseDtoBuilder aSeatResponseDto() {
        return SeatResponseDto.builder()
                .id(ID_100)
                .source(RideSource.FACEBOOK)
                .origin(originCityDto())
                .destination(destinationCityDto())
                .departureTime(LOCAL_DATE.atTime(LOCAL_TIME))
                .isApproximate(false)
                .count(ONE)
                .priceWillingToPay(BIG_DECIMAL)
                .description("Looking for a ride")
                .passenger(aDriverCard().build())
                .contactMethods(List.of(phoneContact()))
                .seatStatus(SeatStatus.SEARCHING);
    }
}
