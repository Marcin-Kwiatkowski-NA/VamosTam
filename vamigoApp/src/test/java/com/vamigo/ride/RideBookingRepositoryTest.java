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
import java.util.Optional;

import static com.vamigo.util.Constants.NON_EXISTENT_ID;
import static com.vamigo.util.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
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
    @DisplayName("Check whether passenger has a booking on a ride in given statuses")
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
    @DisplayName("List bookings on a ride filtered by status")
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
    @DisplayName("List a passenger's bookings filtered by status")
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
    @DisplayName("List bookings with a given status booked before a cutoff")
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
    @DisplayName("List every booking for a ride")
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

    @Nested
    @DisplayName("Load booking with ride, stops and locations eagerly")
    class FindByIdWithRideAndLocationsTests {

        @Test
        @DisplayName("Returns the booking with its ride, every stop and the stop locations initialised")
        void eagerLoadsRideStopsAndLocations() {
            RideBooking booking = createBooking(ride, passenger, BookingStatus.CONFIRMED, Instant.now());

            Optional<RideBooking> found = bookingRepository.findByIdWithRideAndLocations(booking.getId());

            assertThat(found).isPresent();
            RideBooking loaded = found.get();
            assertThat(loaded.getRide().getId()).isEqualTo(ride.getId());
            assertThat(loaded.getRide().getStops())
                    .hasSize(2)
                    .allSatisfy(s -> assertThat(s.getLocation()).isNotNull());
            assertThat(loaded.getBoardStop().getLocation().getId()).isEqualTo(origin.getId());
            assertThat(loaded.getAlightStop().getLocation().getId()).isEqualTo(destination.getId());
        }

        @Test
        @DisplayName("Returns empty when the booking id does not exist")
        void returnsEmptyForMissingId() {
            assertThat(bookingRepository.findByIdWithRideAndLocations(NON_EXISTENT_ID)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Check whether user is a participant in a booking")
    class IsUserParticipantTests {

        @Test
        @DisplayName("Returns true when the user is the driver of the booking's ride")
        void returnsTrueForDriver() {
            RideBooking booking = createBooking(ride, passenger, BookingStatus.CONFIRMED, Instant.now());

            boolean participant = bookingRepository.isUserParticipant(
                    ride.getId(), booking.getId(), driver.getId());

            assertThat(participant).isTrue();
        }

        @Test
        @DisplayName("Returns true when the user is the passenger of the booking")
        void returnsTrueForPassenger() {
            RideBooking booking = createBooking(ride, passenger, BookingStatus.CONFIRMED, Instant.now());

            boolean participant = bookingRepository.isUserParticipant(
                    ride.getId(), booking.getId(), passenger.getId());

            assertThat(participant).isTrue();
        }

        @Test
        @DisplayName("Returns false when the user is neither driver nor passenger")
        void returnsFalseForUnrelatedUser() {
            UserAccount stranger = anActiveUserAccount().email("stranger@example.com").build();
            userAccountRepository.save(stranger);
            RideBooking booking = createBooking(ride, passenger, BookingStatus.CONFIRMED, Instant.now());

            boolean participant = bookingRepository.isUserParticipant(
                    ride.getId(), booking.getId(), stranger.getId());

            assertThat(participant).isFalse();
        }

        @Test
        @DisplayName("Returns false when the booking does not belong to the given ride")
        void returnsFalseWhenBookingBelongsToDifferentRide() {
            Ride other = createPersistedRide();
            RideBooking booking = createBooking(other, passenger, BookingStatus.CONFIRMED, Instant.now());

            boolean participant = bookingRepository.isUserParticipant(
                    ride.getId(), booking.getId(), passenger.getId());

            assertThat(participant).isFalse();
        }
    }

    @Nested
    @DisplayName("List confirmed bookings on recently completed rides")
    class FindConfirmedBookingsOnCompletedRidesTests {

        private void markCompleted(Ride r, Instant completedAt) {
            r.setStatus(Status.COMPLETED);
            r.setCompletedAt(completedAt);
            rideRepository.save(r);
        }

        @Test
        @DisplayName("Returns the passenger's confirmed bookings for rides completed after the cutoff")
        void returnsConfirmedBookingsForPassengerAfterCutoff() {
            RideBooking booking = createBooking(ride, passenger, BookingStatus.CONFIRMED, Instant.now());
            markCompleted(ride, Instant.now().minus(10, ChronoUnit.MINUTES));

            List<RideBooking> result = bookingRepository.findConfirmedBookingsOnCompletedRides(
                    passenger.getId(), Instant.now().minus(1, ChronoUnit.HOURS));

            assertThat(result).extracting(RideBooking::getId).containsExactly(booking.getId());
        }

        @Test
        @DisplayName("Returns the driver's confirmed bookings for rides completed after the cutoff")
        void returnsConfirmedBookingsForDriverAfterCutoff() {
            RideBooking booking = createBooking(ride, passenger, BookingStatus.CONFIRMED, Instant.now());
            markCompleted(ride, Instant.now().minus(10, ChronoUnit.MINUTES));

            List<RideBooking> result = bookingRepository.findConfirmedBookingsOnCompletedRides(
                    driver.getId(), Instant.now().minus(1, ChronoUnit.HOURS));

            assertThat(result).extracting(RideBooking::getId).containsExactly(booking.getId());
        }

        @Test
        @DisplayName("Excludes bookings on rides completed before the cutoff")
        void excludesRidesCompletedBeforeCutoff() {
            createBooking(ride, passenger, BookingStatus.CONFIRMED, Instant.now());
            markCompleted(ride, Instant.now().minus(2, ChronoUnit.HOURS));

            List<RideBooking> result = bookingRepository.findConfirmedBookingsOnCompletedRides(
                    passenger.getId(), Instant.now().minus(1, ChronoUnit.HOURS));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Excludes bookings on rides that are still active")
        void excludesRidesStillActive() {
            createBooking(ride, passenger, BookingStatus.CONFIRMED, Instant.now());

            List<RideBooking> result = bookingRepository.findConfirmedBookingsOnCompletedRides(
                    passenger.getId(), Instant.now().minus(1, ChronoUnit.HOURS));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Excludes bookings that are not in CONFIRMED status")
        void excludesNonConfirmedBookings() {
            createBooking(ride, passenger, BookingStatus.CANCELLED_BY_PASSENGER, Instant.now());
            markCompleted(ride, Instant.now().minus(10, ChronoUnit.MINUTES));

            List<RideBooking> result = bookingRepository.findConfirmedBookingsOnCompletedRides(
                    passenger.getId(), Instant.now().minus(1, ChronoUnit.HOURS));

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Excludes bookings when the user is neither driver nor passenger")
        void excludesUnrelatedUser() {
            UserAccount stranger = anActiveUserAccount().email("stranger-ride@example.com").build();
            userAccountRepository.save(stranger);
            createBooking(ride, passenger, BookingStatus.CONFIRMED, Instant.now());
            markCompleted(ride, Instant.now().minus(10, ChronoUnit.MINUTES));

            List<RideBooking> result = bookingRepository.findConfirmedBookingsOnCompletedRides(
                    stranger.getId(), Instant.now().minus(1, ChronoUnit.HOURS));

            assertThat(result).isEmpty();
        }
    }
}
