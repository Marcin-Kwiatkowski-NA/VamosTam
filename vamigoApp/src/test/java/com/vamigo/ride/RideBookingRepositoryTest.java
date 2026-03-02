package com.vamigo.ride;

import com.vamigo.AbstractIntegrationTest;
import com.vamigo.domain.Status;
import com.vamigo.location.Location;
import com.vamigo.location.LocationRepository;
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
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static com.vamigo.util.TestFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class RideBookingRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private RideBookingRepository bookingRepository;

    @Autowired
    private RideRepository rideRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    private UserAccount driver;
    private UserAccount passenger;
    private Vehicle vehicle;
    private Location origin;
    private Location destination;
    private Ride ride;

    @BeforeEach
    void setUp() {
        origin = anOriginLocation().id(null).build();
        destination = aDestinationLocation().id(null).build();
        locationRepository.save(origin);
        locationRepository.save(destination);

        driver = anActiveUserAccount().build();
        userAccountRepository.save(driver);
        userProfileRepository.save(aUserProfile(driver).build());

        passenger = anActiveUserAccount().email("passenger@example.com").build();
        userAccountRepository.save(passenger);
        userProfileRepository.save(aUserProfile(passenger).displayName("Passenger").build());

        vehicle = aTesla().id(null).owner(driver).build();
        vehicleRepository.save(vehicle);

        ride = createPersistedRide();
    }

    private Ride createPersistedRide() {
        Ride r = aRide(origin, destination)
                .id(null)
                .driver(driver)
                .vehicle(vehicle)
                .status(Status.ACTIVE)
                .autoApprove(true)
                .stops(new ArrayList<>())
                .bookings(new ArrayList<>())
                .build();
        r.getStops().addAll(buildStops(r, origin, destination));
        return rideRepository.save(r);
    }

    private RideBooking createBooking(Ride r, UserAccount pax, BookingStatus status, Instant bookedAt) {
        RideBooking booking = RideBooking.builder()
                .ride(r)
                .passenger(pax)
                .boardStop(r.getStops().get(0))
                .alightStop(r.getStops().get(r.getStops().size() - 1))
                .status(status)
                .seatCount(1)
                .bookedAt(bookedAt)
                .resolvedAt(status == BookingStatus.CONFIRMED ? bookedAt : null)
                .build();
        return bookingRepository.save(booking);
    }

    @Nested
    @DisplayName("existsByRideIdAndPassengerIdAndStatusIn")
    class ExistsByStatusTests {

        @Test
        @DisplayName("Returns true when active booking exists")
        void returnsTrueForActiveBooking() {
            createBooking(ride, passenger, BookingStatus.CONFIRMED, Instant.now());

            boolean exists = bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(
                    ride.getId(), passenger.getId(),
                    List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));

            assertTrue(exists);
        }

        @Test
        @DisplayName("Returns false when only cancelled booking exists")
        void returnsFalseForCancelledBooking() {
            createBooking(ride, passenger, BookingStatus.CANCELLED_BY_PASSENGER, Instant.now());

            boolean exists = bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(
                    ride.getId(), passenger.getId(),
                    List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));

            assertFalse(exists);
        }

        @Test
        @DisplayName("Returns false when no booking exists")
        void returnsFalseWhenNoBooking() {
            boolean exists = bookingRepository.existsByRideIdAndPassengerIdAndStatusIn(
                    ride.getId(), passenger.getId(),
                    List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));

            assertFalse(exists);
        }
    }

    @Nested
    @DisplayName("findByRideIdAndStatusIn")
    class FindByRideIdAndStatusTests {

        @Test
        @DisplayName("Returns only bookings with matching status")
        void returnsOnlyMatchingStatuses() {
            createBooking(ride, passenger, BookingStatus.CONFIRMED, Instant.now());

            UserAccount otherPassenger = anActiveUserAccount().email("other@example.com").build();
            userAccountRepository.save(otherPassenger);
            createBooking(ride, otherPassenger, BookingStatus.CANCELLED_BY_PASSENGER, Instant.now());

            List<RideBooking> active = bookingRepository.findByRideIdAndStatusIn(
                    ride.getId(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));

            assertEquals(1, active.size());
            assertEquals(BookingStatus.CONFIRMED, active.get(0).getStatus());
        }

        @Test
        @DisplayName("Returns empty list when no bookings match")
        void returnsEmptyWhenNoMatch() {
            createBooking(ride, passenger, BookingStatus.REJECTED, Instant.now());

            List<RideBooking> active = bookingRepository.findByRideIdAndStatusIn(
                    ride.getId(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));

            assertTrue(active.isEmpty());
        }
    }

    @Nested
    @DisplayName("findByPassengerIdAndStatusIn")
    class FindByPassengerIdAndStatusTests {

        @Test
        @DisplayName("Returns active bookings for passenger across rides")
        void returnsActiveBookingsAcrossRides() {
            Ride ride2 = createPersistedRide();

            createBooking(ride, passenger, BookingStatus.CONFIRMED, Instant.now());
            createBooking(ride2, passenger, BookingStatus.PENDING, Instant.now());

            List<RideBooking> active = bookingRepository.findByPassengerIdAndStatusIn(
                    passenger.getId(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));

            assertEquals(2, active.size());
        }

        @Test
        @DisplayName("Excludes non-active bookings")
        void excludesNonActive() {
            createBooking(ride, passenger, BookingStatus.CONFIRMED, Instant.now());
            Ride ride2 = createPersistedRide();
            createBooking(ride2, passenger, BookingStatus.EXPIRED, Instant.now());

            List<RideBooking> active = bookingRepository.findByPassengerIdAndStatusIn(
                    passenger.getId(), List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED));

            assertEquals(1, active.size());
            assertEquals(BookingStatus.CONFIRMED, active.get(0).getStatus());
        }
    }

    @Nested
    @DisplayName("findByStatusAndBookedAtBefore")
    class FindByStatusAndBookedAtBeforeTests {

        @Test
        @DisplayName("Returns PENDING bookings older than cutoff")
        void returnsStaleBookings() {
            Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);
            createBooking(ride, passenger, BookingStatus.PENDING, twoHoursAgo);

            Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
            List<RideBooking> stale = bookingRepository.findByStatusAndBookedAtBefore(
                    BookingStatus.PENDING, cutoff);

            assertEquals(1, stale.size());
        }

        @Test
        @DisplayName("Excludes PENDING bookings newer than cutoff")
        void excludesRecentBookings() {
            createBooking(ride, passenger, BookingStatus.PENDING, Instant.now());

            Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
            List<RideBooking> stale = bookingRepository.findByStatusAndBookedAtBefore(
                    BookingStatus.PENDING, cutoff);

            assertTrue(stale.isEmpty());
        }

        @Test
        @DisplayName("Excludes non-PENDING bookings even if old")
        void excludesNonPendingOldBookings() {
            Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);
            createBooking(ride, passenger, BookingStatus.CONFIRMED, twoHoursAgo);

            Instant cutoff = Instant.now().minus(1, ChronoUnit.HOURS);
            List<RideBooking> stale = bookingRepository.findByStatusAndBookedAtBefore(
                    BookingStatus.PENDING, cutoff);

            assertTrue(stale.isEmpty());
        }
    }

    @Nested
    @DisplayName("findByRideId")
    class FindByRideIdTests {

        @Test
        @DisplayName("Returns all bookings for a ride regardless of status")
        void returnsAllBookingsForRide() {
            createBooking(ride, passenger, BookingStatus.CONFIRMED, Instant.now());

            UserAccount otherPassenger = anActiveUserAccount().email("other2@example.com").build();
            userAccountRepository.save(otherPassenger);
            createBooking(ride, otherPassenger, BookingStatus.CANCELLED_BY_PASSENGER, Instant.now());

            List<RideBooking> bookings = bookingRepository.findByRideId(ride.getId());

            assertEquals(2, bookings.size());
        }

        @Test
        @DisplayName("Returns empty list when ride has no bookings")
        void returnsEmptyForRideWithNoBookings() {
            List<RideBooking> bookings = bookingRepository.findByRideId(ride.getId());

            assertTrue(bookings.isEmpty());
        }
    }
}
