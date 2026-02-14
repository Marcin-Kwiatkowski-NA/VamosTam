package com.blablatwo.util;

import com.blablatwo.dto.ContactMethodDto;
import com.blablatwo.dto.ContactType;
import com.blablatwo.dto.UserCardDto;
import com.blablatwo.location.Location;
import com.blablatwo.location.LocationDto;
import com.blablatwo.location.LocationRef;
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
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import java.util.List;
import java.util.Set;

import static com.blablatwo.util.Constants.*;

public final class TestFixtures {

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    private TestFixtures() {}

    // ──────────────── Location ────────────────

    public static Location.LocationBuilder anOriginLocation() {
        return Location.builder()
                .id(ID_ONE)
                .osmId(OSM_ID_ORIGIN)
                .name(LOCATION_NAME_ORIGIN)
                .countryCode("PL")
                .coordinates(GF.createPoint(new Coordinate(LON_ORIGIN, LAT_ORIGIN)));
    }

    public static Location.LocationBuilder aDestinationLocation() {
        return Location.builder()
                .id(2L)
                .osmId(OSM_ID_DESTINATION)
                .name(LOCATION_NAME_DESTINATION)
                .countryCode("PL")
                .coordinates(GF.createPoint(new Coordinate(LON_DESTINATION, LAT_DESTINATION)));
    }

    public static Location.LocationBuilder aKrakowLocation() {
        return Location.builder()
                .osmId(OSM_ID_KRAKOW)
                .name(LOCATION_NAME_KRAKOW)
                .countryCode("PL")
                .coordinates(GF.createPoint(new Coordinate(LON_KRAKOW, LAT_KRAKOW)));
    }

    public static Location.LocationBuilder aWarsawLocation() {
        return Location.builder()
                .osmId(OSM_ID_WARSAW)
                .name(LOCATION_NAME_WARSAW)
                .countryCode("PL")
                .coordinates(GF.createPoint(new Coordinate(LON_WARSAW, LAT_WARSAW)));
    }

    public static LocationDto originLocationDto() {
        return new LocationDto(OSM_ID_ORIGIN, LOCATION_NAME_ORIGIN, null, "PL", LAT_ORIGIN, LON_ORIGIN, null);
    }

    public static LocationDto destinationLocationDto() {
        return new LocationDto(OSM_ID_DESTINATION, LOCATION_NAME_DESTINATION, null, "PL", LAT_DESTINATION, LON_DESTINATION, null);
    }

    public static LocationDto krakowLocationDto() {
        return new LocationDto(OSM_ID_KRAKOW, LOCATION_NAME_KRAKOW, null, "PL", LAT_KRAKOW, LON_KRAKOW, null);
    }

    public static LocationDto warsawLocationDto() {
        return new LocationDto(OSM_ID_WARSAW, LOCATION_NAME_WARSAW, null, "PL", LAT_WARSAW, LON_WARSAW, null);
    }

    public static LocationDto parisLocationDto() {
        return new LocationDto(OSM_ID_PARIS, LOCATION_NAME_PARIS, null, "FR", 48.8566, 2.3522, null);
    }

    public static LocationDto lyonLocationDto() {
        return new LocationDto(OSM_ID_LYON, LOCATION_NAME_LYON, null, "FR", 45.7640, 4.8357, null);
    }

    public static LocationRef originLocationRef() {
        return new LocationRef(OSM_ID_ORIGIN, LOCATION_NAME_ORIGIN, LAT_ORIGIN, LON_ORIGIN,
                "PL", null, null, null, null, null, null, null, null);
    }

    public static LocationRef destinationLocationRef() {
        return new LocationRef(OSM_ID_DESTINATION, LOCATION_NAME_DESTINATION, LAT_DESTINATION, LON_DESTINATION,
                "PL", null, null, null, null, null, null, null, null);
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
                .origin(originLocationRef())
                .destination(destinationLocationRef())
                .departureTime(LOCAL_DATE_TIME)
                .isApproximate(false)
                .availableSeats(ONE)
                .pricePerSeat(BIG_DECIMAL)
                .vehicleId(ID_ONE)
                .description(RIDE_DESCRIPTION);
    }

    public static Ride.RideBuilder<?, ?> aRide(Location origin, Location destination) {
        return Ride.builder()
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
    }

    public static List<RideStop> buildStops(Ride ride, Location origin, Location destination) {
        return List.of(
                RideStop.builder().ride(ride).location(origin).stopOrder(0)
                        .departureTime(LOCAL_DATE.atTime(LOCAL_TIME)).build(),
                RideStop.builder().ride(ride).location(destination).stopOrder(1)
                        .departureTime(null).build()
        );
    }

    public static Ride buildRideWithStops(Location origin, Location destination) {
        Ride ride = aRide(origin, destination).build();
        ride.setStops(new java.util.ArrayList<>(buildStops(ride, origin, destination)));
        return ride;
    }

    public static Ride buildRideWithStops() {
        return buildRideWithStops(anOriginLocation().build(), aDestinationLocation().build());
    }

    public static Ride.RideBuilder<?, ?> aRide() {
        return aRide(anOriginLocation().build(), aDestinationLocation().build());
    }

    public static BookRideRequest.BookRideRequestBuilder aBookRideRequest() {
        return BookRideRequest.builder()
                .boardStopOsmId(OSM_ID_ORIGIN)
                .alightStopOsmId(OSM_ID_DESTINATION);
    }

    public static RideResponseDto.RideResponseDtoBuilder aRideResponseDto() {
        return RideResponseDto.builder()
                .id(ID_100)
                .source(RideSource.INTERNAL)
                .origin(originLocationDto())
                .destination(destinationLocationDto())
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
                .origin(parisLocationDto())
                .destination(lyonLocationDto())
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
                .originLocationName(LOCATION_NAME_ORIGIN)
                .destinationLocationName(LOCATION_NAME_DESTINATION)
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
                .intermediateStopLocationNames(List.of(LOCATION_NAME_KRAKOW));
    }

    // ── French External Ride Scenario ──

    public static ExternalRideCreationDto.ExternalRideCreationDtoBuilder aFrenchRideCreation() {
        return anExternalRideCreationDto()
                .originLocationName(LOCATION_NAME_PARIS)
                .destinationLocationName(LOCATION_NAME_LYON)
                .availableSeats(3)
                .pricePerSeat(PRICE_25)
                .description(FRENCH_RIDE_DESCRIPTION)
                .phoneNumber(TELEPHONE_FR)
                .authorName(JEAN_DUPONT)
                .sourceUrl(FACEBOOK_GROUP_URL);
    }

    public static ExternalSeatCreationDto.ExternalSeatCreationDtoBuilder anExternalSeatCreationDto() {
        return ExternalSeatCreationDto.builder()
                .originLocationName(LOCATION_NAME_ORIGIN)
                .destinationLocationName(LOCATION_NAME_DESTINATION)
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
                .origin(originLocationDto())
                .destination(destinationLocationDto())
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
