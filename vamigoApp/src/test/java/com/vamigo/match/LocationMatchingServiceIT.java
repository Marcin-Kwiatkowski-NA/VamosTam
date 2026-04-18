package com.vamigo.match;

import com.vamigo.AbstractIntegrationTest;
import com.vamigo.domain.Status;
import com.vamigo.location.Location;
import com.vamigo.location.LocationDto;
import com.vamigo.location.LocationRepository;
import com.vamigo.ride.Ride;
import com.vamigo.ride.RideRepository;
import com.vamigo.ride.RideStop;
import com.vamigo.ride.dto.RideResponseDto;
import com.vamigo.ride.dto.RideStopDto;
import com.vamigo.searchalert.SavedSearch;
import com.vamigo.searchalert.SavedSearchRepository;
import com.vamigo.searchalert.SearchType;
import com.vamigo.seat.Seat;
import com.vamigo.seat.SeatRepository;
import com.vamigo.seat.dto.SeatResponseDto;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.UserProfileRepository;
import com.vamigo.vehicle.Vehicle;
import com.vamigo.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static com.vamigo.util.Constants.*;
import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LocationMatchingServiceIT extends AbstractIntegrationTest {

    @Autowired private LocationMatchingService service;
    @Autowired private RideRepository rideRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private SavedSearchRepository savedSearchRepository;

    private UserAccount driver;
    private UserAccount watcher;
    private Vehicle vehicle;
    private Location krakow;
    private Location warsaw;
    private Location nearKrakow;   // ~5km from Krakow
    private Location farFromBoth;  // hundreds of km from Warsaw

    private static final GeoPoint KRAKOW_PT = new GeoPoint(LAT_KRAKOW, LON_KRAKOW);
    private static final GeoPoint WARSAW_PT = new GeoPoint(LAT_WARSAW, LON_WARSAW);

    private static final RadiusStrategy TIGHT = RadiusStrategy.fixedKm(10);
    private static final RadiusStrategy WIDE  = RadiusStrategy.fixedKm(50);

    @BeforeEach
    void setUp() {
        krakow = locationRepository.save(aKrakowLocation().id(null).build());
        warsaw = locationRepository.save(aWarsawLocation().id(null).build());
        // ~5 km north of Krakow
        nearKrakow = locationRepository.save(
                aKrakowLocation().id(null).osmId(999001L)
                        .namePl("Near Krakow").nameEn("Near Krakow")
                        .coordinates(point(LAT_KRAKOW + 0.045, LON_KRAKOW))
                        .build());
        // well outside any 50km radius from Warsaw
        farFromBoth = locationRepository.save(
                aWarsawLocation().id(null).osmId(999002L)
                        .namePl("Far").nameEn("Far")
                        .coordinates(point(48.0, 16.0))
                        .build());

        driver = userAccountRepository.save(
                anActiveUserAccount().email("driver@example.com").build());
        userProfileRepository.save(aUserProfile(driver).build());

        watcher = userAccountRepository.save(
                anActiveUserAccount().email("watcher@example.com").build());

        vehicle = vehicleRepository.save(aTesla().id(null).owner(driver).build());
    }

    // =====================================================================
    //   findRides (forward)
    // =====================================================================

    @Nested
    @DisplayName("findRides — forward matching on stop-aware routes")
    class FindRidesTests {

        @Test
        @DisplayName("Returns rides whose stops straddle the origin → destination query")
        void returnsStraddlingRide() {
            Ride ride = persistRide(Instant.now().plus(1, ChronoUnit.DAYS), krakow, warsaw);

            var query = new RideMatchQuery(
                    new RouteQuery(KRAKOW_PT, WARSAW_PT, krakow.getOsmId(), warsaw.getOsmId()),
                    TIGHT, null, null);

            var page = service.findRides(query, PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(m -> m.ride().getId())
                    .containsExactly(ride.getId());
        }

        @Test
        @DisplayName("Excludes rides whose stops are outside the configured radius")
        void excludesRideOutsideRadius() {
            persistRide(Instant.now().plus(1, ChronoUnit.DAYS), farFromBoth, warsaw);

            var query = new RideMatchQuery(
                    new RouteQuery(KRAKOW_PT, WARSAW_PT, krakow.getOsmId(), warsaw.getOsmId()),
                    TIGHT, null, null);

            var page = service.findRides(query, PageRequest.of(0, 10));

            assertThat(page.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Excludes rides that are not ACTIVE")
        void excludesNonActiveRides() {
            Ride ride = persistRide(Instant.now().plus(1, ChronoUnit.DAYS), krakow, warsaw);
            ride.setStatus(Status.CANCELLED);
            rideRepository.saveAndFlush(ride);

            var query = new RideMatchQuery(
                    new RouteQuery(KRAKOW_PT, WARSAW_PT, krakow.getOsmId(), warsaw.getOsmId()),
                    TIGHT, null, null);

            var page = service.findRides(query, PageRequest.of(0, 10));

            assertThat(page.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Date window restricts rides to the earliest / latest bounds")
        void respectsDateWindow() {
            Instant departure = Instant.now().plus(3, ChronoUnit.DAYS);
            persistRide(departure, krakow, warsaw);

            // window ending before the departure → no hit
            var narrowQuery = new RideMatchQuery(
                    new RouteQuery(KRAKOW_PT, WARSAW_PT, krakow.getOsmId(), warsaw.getOsmId()),
                    TIGHT,
                    DateWindow.of(Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS)),
                    null);
            assertThat(service.findRides(narrowQuery, PageRequest.of(0, 10)).getContent())
                    .isEmpty();

            // window covering the departure → hit
            var wideQuery = new RideMatchQuery(
                    new RouteQuery(KRAKOW_PT, WARSAW_PT, krakow.getOsmId(), warsaw.getOsmId()),
                    TIGHT,
                    DateWindow.of(Instant.now(), Instant.now().plus(7, ChronoUnit.DAYS)),
                    null);
            assertThat(wideQuery).isNotNull();
            assertThat(service.findRides(wideQuery, PageRequest.of(0, 10)).getContent())
                    .hasSize(1);
        }

        @Test
        @DisplayName("Driver filter returns only rides driven by the given user")
        void filtersByDriver() {
            Ride mine = persistRide(Instant.now().plus(1, ChronoUnit.DAYS), krakow, warsaw);
            UserAccount otherDriver = userAccountRepository.save(
                    anActiveUserAccount().email("other-driver@example.com").build());
            userProfileRepository.save(aUserProfile(otherDriver).build());
            Vehicle otherVehicle = vehicleRepository.save(
                    aTesla().id(null).licensePlate("OTH-123").owner(otherDriver).build());
            persistRide(Instant.now().plus(1, ChronoUnit.DAYS), krakow, warsaw, otherDriver, otherVehicle);

            var query = new RideMatchQuery(
                    new RouteQuery(KRAKOW_PT, WARSAW_PT, krakow.getOsmId(), warsaw.getOsmId()),
                    TIGHT,
                    null,
                    MatchFilters.forDriver(driver.getId()));

            var page = service.findRides(query, PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(m -> m.ride().getId())
                    .containsExactly(mine.getId());
        }

        @Test
        @DisplayName("Orders results by summed origin + destination stop distance (nearest first)")
        void ordersByCombinedDistance() {
            Ride farther = persistRide(Instant.now().plus(1, ChronoUnit.DAYS), nearKrakow, warsaw);
            Ride nearer  = persistRide(Instant.now().plus(1, ChronoUnit.DAYS), krakow, warsaw);

            var query = new RideMatchQuery(
                    new RouteQuery(KRAKOW_PT, WARSAW_PT, null, null),
                    WIDE, null, null);

            var page = service.findRides(query, PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(m -> m.ride().getId())
                    .containsExactly(nearer.getId(), farther.getId());
        }
    }

    // =====================================================================
    //   findSeats (forward)
    // =====================================================================

    @Nested
    @DisplayName("findSeats — forward matching on point-to-point seats")
    class FindSeatsTests {

        @Test
        @DisplayName("Returns seats whose origin and destination are both within radius")
        void returnsSeatsInRange() {
            Seat seat = persistSeat(krakow, warsaw, Instant.now().plus(1, ChronoUnit.DAYS));

            var query = new SeatMatchQuery(
                    new RouteQuery(KRAKOW_PT, WARSAW_PT, krakow.getOsmId(), warsaw.getOsmId()),
                    TIGHT, null, null);

            var page = service.findSeats(query, PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(m -> m.seat().getId())
                    .containsExactly(seat.getId());
        }

        @Test
        @DisplayName("Excludes seats with origin outside the radius")
        void excludesOutOfRangeOrigin() {
            persistSeat(farFromBoth, warsaw, Instant.now().plus(1, ChronoUnit.DAYS));

            var query = new SeatMatchQuery(
                    new RouteQuery(KRAKOW_PT, WARSAW_PT, krakow.getOsmId(), warsaw.getOsmId()),
                    TIGHT, null, null);

            assertThat(service.findSeats(query, PageRequest.of(0, 10)).getContent()).isEmpty();
        }

        @Test
        @DisplayName("Orders seats by summed origin + destination distance")
        void ordersByCombinedDistance() {
            Seat farther = persistSeat(nearKrakow, warsaw, Instant.now().plus(1, ChronoUnit.DAYS));
            Seat nearer  = persistSeat(krakow, warsaw, Instant.now().plus(1, ChronoUnit.DAYS));

            var query = new SeatMatchQuery(
                    new RouteQuery(KRAKOW_PT, WARSAW_PT, null, null),
                    WIDE, null, null);

            var page = service.findSeats(query, PageRequest.of(0, 10));

            assertThat(page.getContent())
                    .extracting(m -> m.seat().getId())
                    .containsExactly(nearer.getId(), farther.getId());
        }

        @Test
        @DisplayName("Date window restricts seats to departures within [earliest, latest]")
        void respectsDateWindow() {
            Instant far = Instant.now().plus(3, ChronoUnit.DAYS);
            persistSeat(krakow, warsaw, far);

            var narrow = new SeatMatchQuery(
                    new RouteQuery(KRAKOW_PT, WARSAW_PT, null, null),
                    TIGHT,
                    DateWindow.of(Instant.now(), Instant.now().plus(1, ChronoUnit.DAYS)),
                    null);
            assertThat(service.findSeats(narrow, PageRequest.of(0, 10)).getContent()).isEmpty();
        }
    }

    // =====================================================================
    //   findSavedSearchesMatchingRide (inverse)
    // =====================================================================

    @Nested
    @DisplayName("findSavedSearchesMatchingRide — inverse matching on saved searches")
    class SavedSearchMatchingRideTests {

        @Test
        @DisplayName("Flags exact matches with null distances and null names")
        void exactMatchProducesNullDistancesAndNames() {
            Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
            SavedSearch ss = persistSavedSearch(watcher, krakow, warsaw, departure);

            RideResponseDto ride = rideDtoFromStops(departure, krakow, warsaw);

            List<SavedSearchMatch> matches = service.findSavedSearchesMatchingRide(ride, driver.getId());

            assertThat(matches)
                    .extracting(SavedSearchMatch::savedSearchId,
                            SavedSearchMatch::exactMatch,
                            SavedSearchMatch::originDistanceM,
                            SavedSearchMatch::destinationDistanceM,
                            SavedSearchMatch::originStopName,
                            SavedSearchMatch::destinationStopName)
                    .containsExactly(tuple(ss.getId(), true, null, null, null, null));
        }

        @Test
        @DisplayName("Proximity matches carry distances and the ride stop names")
        void proximityMatchCarriesDistancesAndStopNames() {
            Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
            SavedSearch ss = persistSavedSearch(watcher, krakow, warsaw, departure);

            // Ride does NOT hit the saved search's osm ids — forces proximity branch.
            RideResponseDto ride = rideDtoFromStops(departure, nearKrakow, warsaw);

            List<SavedSearchMatch> matches = service.findSavedSearchesMatchingRide(ride, driver.getId());

            assertThat(matches).hasSize(1);
            SavedSearchMatch match = matches.get(0);
            assertThat(match.savedSearchId()).isEqualTo(ss.getId());
            assertThat(match.exactMatch()).isFalse();
            assertThat(match.originDistanceM()).isGreaterThan(0);
            assertThat(match.destinationDistanceM()).isEqualTo(0);
            assertThat(match.originStopName()).isEqualTo(nearKrakow.getNameEn());
            assertThat(match.destinationStopName()).isEqualTo(warsaw.getNameEn());
        }

        @Test
        @DisplayName("Excludes saved searches belonging to the ride's own driver")
        void excludesDriversOwnSavedSearch() {
            Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
            persistSavedSearch(driver, krakow, warsaw, departure);

            RideResponseDto ride = rideDtoFromStops(departure, krakow, warsaw);

            assertThat(service.findSavedSearchesMatchingRide(ride, driver.getId())).isEmpty();
        }

        @Test
        @DisplayName("Ignores saved searches whose departure date differs from the ride's")
        void ignoresDifferentDate() {
            Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
            persistSavedSearch(watcher, krakow, warsaw,
                    departure.plus(7, ChronoUnit.DAYS));

            RideResponseDto ride = rideDtoFromStops(departure, krakow, warsaw);

            assertThat(service.findSavedSearchesMatchingRide(ride, driver.getId())).isEmpty();
        }

        @Test
        @DisplayName("Multi-stop ride matches a saved search via an intermediate stop pair")
        void multiStopRideMatchesIntermediatePair() {
            Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
            Location gdansk = locationRepository.save(aKrakowLocation().id(null)
                    .osmId(999003L).namePl("Gdansk").nameEn("Gdansk")
                    .coordinates(point(54.352, 18.6466)).build());

            SavedSearch ss = persistSavedSearch(watcher, krakow, warsaw, departure);

            // Gdansk → Krakow → Warsaw: saved search matches the (krakow, warsaw) pair.
            RideResponseDto ride = rideDtoFromStops(departure, gdansk, krakow, warsaw);

            List<SavedSearchMatch> matches = service.findSavedSearchesMatchingRide(ride, driver.getId());

            assertThat(matches)
                    .extracting(SavedSearchMatch::savedSearchId, SavedSearchMatch::exactMatch)
                    .containsExactly(tuple(ss.getId(), true));
        }

        @Test
        @DisplayName("Keeps the closest proximity candidate when several stop pairs hit the same saved search")
        void keepsBestProximityCandidate() {
            Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
            // Saved search is on osm ids 999004/999005 — no exact match possible.
            Location destWatched = locationRepository.save(aWarsawLocation().id(null)
                    .osmId(999005L).namePl("Dest Watched").nameEn("Dest Watched")
                    .coordinates(point(LAT_WARSAW, LON_WARSAW)).build());
            Location originWatched = locationRepository.save(aKrakowLocation().id(null)
                    .osmId(999004L).namePl("Origin Watched").nameEn("Origin Watched")
                    .coordinates(point(LAT_KRAKOW, LON_KRAKOW)).build());
            SavedSearch ss = persistSavedSearch(watcher, originWatched, destWatched, departure);

            // Two stop pairs hit the search: (nearKrakow, warsaw) ≈ 5 km origin offset,
            // (krakow, warsaw) ≈ 0 m offset. Expect the 0 m pair to win.
            RideResponseDto ride = rideDtoFromStops(departure, nearKrakow, krakow, warsaw);

            List<SavedSearchMatch> matches = service.findSavedSearchesMatchingRide(ride, driver.getId());

            assertThat(matches).hasSize(1);
            SavedSearchMatch match = matches.get(0);
            assertThat(match.savedSearchId()).isEqualTo(ss.getId());
            assertThat(match.exactMatch()).isFalse();
            assertThat(match.combinedDistanceM()).isLessThan(100);
            assertThat(match.originStopName()).isEqualTo(krakow.getNameEn());
            assertThat(match.destinationStopName()).isEqualTo(warsaw.getNameEn());
        }

        @Test
        @DisplayName("Returns an empty list when the ride has fewer than two stops")
        void emptyWhenRideHasInsufficientStops() {
            Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
            RideResponseDto ride = rideDtoFromStops(departure, krakow);

            assertThat(service.findSavedSearchesMatchingRide(ride, driver.getId())).isEmpty();
        }
    }

    // =====================================================================
    //   findSavedSearchesMatchingSeat (inverse)
    // =====================================================================

    @Nested
    @DisplayName("findSavedSearchesMatchingSeat — inverse matching on seats")
    class SavedSearchMatchingSeatTests {

        @Test
        @DisplayName("Returns saved searches with exact-match flag when both osm ids match")
        void exactMatch() {
            Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
            SavedSearch ss = persistSavedSearch(watcher, krakow, warsaw, departure, SearchType.SEAT);

            SeatResponseDto seat = seatDto(departure, krakow, warsaw);

            List<SavedSearchMatch> matches = service.findSavedSearchesMatchingSeat(seat, driver.getId());

            assertThat(matches)
                    .extracting(SavedSearchMatch::savedSearchId, SavedSearchMatch::exactMatch)
                    .containsExactly(tuple(ss.getId(), true));
        }

        @Test
        @DisplayName("Excludes saved searches belonging to the seat's own creator")
        void excludesCreator() {
            Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
            persistSavedSearch(driver, krakow, warsaw, departure, SearchType.SEAT);

            SeatResponseDto seat = seatDto(departure, krakow, warsaw);

            assertThat(service.findSavedSearchesMatchingSeat(seat, driver.getId())).isEmpty();
        }

        @Test
        @DisplayName("Proximity match records distances and seat endpoint names")
        void proximityMatch() {
            Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
            SavedSearch ss = persistSavedSearch(watcher, krakow, warsaw, departure, SearchType.SEAT);

            SeatResponseDto seat = seatDto(departure, nearKrakow, warsaw);

            List<SavedSearchMatch> matches = service.findSavedSearchesMatchingSeat(seat, driver.getId());

            assertThat(matches).hasSize(1);
            SavedSearchMatch match = matches.get(0);
            assertThat(match.savedSearchId()).isEqualTo(ss.getId());
            assertThat(match.exactMatch()).isFalse();
            assertThat(match.originDistanceM()).isGreaterThan(0);
            assertThat(match.destinationDistanceM()).isEqualTo(0);
            assertThat(match.originStopName()).isEqualTo(nearKrakow.getNameEn());
            assertThat(match.destinationStopName()).isEqualTo(warsaw.getNameEn());
        }

        @Test
        @DisplayName("Returns empty when either seat endpoint is missing")
        void emptyForIncompleteSeat() {
            Instant departure = Instant.now().plus(2, ChronoUnit.DAYS);
            SeatResponseDto missingDest = aSeatResponseDto()
                    .departureTime(departure).origin(toDto(krakow)).destination(null).build();

            assertThat(service.findSavedSearchesMatchingSeat(missingDest, driver.getId())).isEmpty();
        }
    }

    // =====================================================================
    //   Helpers
    // =====================================================================

    private Ride persistRide(Instant departure, Location origin, Location dest) {
        return persistRide(departure, origin, dest, driver, vehicle, origin, dest);
    }

    private Ride persistRide(Instant departure, Location origin, Location dest,
                             UserAccount drv, Vehicle veh) {
        return persistRide(departure, origin, dest, drv, veh, origin, dest);
    }

    /** Persist a ride whose stops are exactly the given locations, in order. */
    private Ride persistRide(Instant departure, Location origin, Location dest,
                             UserAccount drv, Vehicle veh,
                             Location... stopLocations) {
        Ride ride = aRide(origin, dest)
                .id(null).driver(drv).vehicle(veh).status(Status.ACTIVE)
                .departureTime(departure)
                .stops(new ArrayList<>())
                .build();
        for (int i = 0; i < stopLocations.length; i++) {
            ride.getStops().add(RideStop.builder()
                    .ride(ride).location(stopLocations[i])
                    .stopOrder(i)
                    .departureTime(i == 0 ? departure : null)
                    .build());
        }
        return rideRepository.saveAndFlush(ride);
    }

    private Seat persistSeat(Location origin, Location destination, Instant departure) {
        Seat seat = aSeat(driver, origin, destination)
                .departureTime(departure)
                .status(Status.ACTIVE)
                .build();
        return seatRepository.saveAndFlush(seat);
    }

    private SavedSearch persistSavedSearch(UserAccount user,
                                           Location origin, Location destination,
                                           Instant departure) {
        return persistSavedSearch(user, origin, destination, departure, SearchType.RIDE);
    }

    private SavedSearch persistSavedSearch(UserAccount user,
                                           Location origin, Location destination,
                                           Instant departure, SearchType type) {
        SavedSearch ss = SavedSearch.builder()
                .user(user)
                .originOsmId(origin.getOsmId())
                .originName(origin.getNameEn())
                .originLat(origin.getCoordinates().getY())
                .originLon(origin.getCoordinates().getX())
                .destinationOsmId(destination.getOsmId())
                .destinationName(destination.getNameEn())
                .destinationLat(destination.getCoordinates().getY())
                .destinationLon(destination.getCoordinates().getX())
                .searchType(type)
                .departureDate(departure.atOffset(ZoneOffset.UTC).toLocalDate())
                .active(true)
                .build();
        return savedSearchRepository.saveAndFlush(ss);
    }

    private RideResponseDto rideDtoFromStops(Instant departure, Location... stops) {
        List<RideStopDto> stopDtos = new ArrayList<>();
        for (int i = 0; i < stops.length; i++) {
            stopDtos.add(new RideStopDto(i, toDto(stops[i]),
                    i == 0 ? departure : null, null));
        }
        LocationDto originDto = stops.length > 0 ? toDto(stops[0]) : null;
        LocationDto destDto   = stops.length > 0 ? toDto(stops[stops.length - 1]) : null;
        return aRideResponseDto()
                .origin(originDto).destination(destDto)
                .stops(stopDtos)
                .departureTime(departure)
                .build();
    }

    private SeatResponseDto seatDto(Instant departure, Location origin, Location destination) {
        return aSeatResponseDto()
                .origin(toDto(origin)).destination(toDto(destination))
                .departureTime(departure)
                .build();
    }

    private static LocationDto toDto(Location l) {
        return new LocationDto(
                l.getOsmId(), l.getNameEn(), null, "PL",
                l.getCoordinates().getY(), l.getCoordinates().getX(), null);
    }

    private static org.locationtech.jts.geom.Point point(double lat, double lon) {
        var gf = new org.locationtech.jts.geom.GeometryFactory(
                new org.locationtech.jts.geom.PrecisionModel(), 4326);
        return gf.createPoint(new org.locationtech.jts.geom.Coordinate(lon, lat));
    }
}
