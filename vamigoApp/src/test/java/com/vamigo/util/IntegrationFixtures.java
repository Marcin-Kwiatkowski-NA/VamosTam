package com.vamigo.util;

import com.vamigo.auth.service.JwtTokenProvider;
import com.vamigo.domain.Currency;
import com.vamigo.domain.TimePrecision;
import com.vamigo.location.Location;
import com.vamigo.location.LocationRepository;
import com.vamigo.ride.BookingStatus;
import com.vamigo.ride.Ride;
import com.vamigo.ride.RideBooking;
import com.vamigo.ride.RideBookingRepository;
import com.vamigo.ride.RideRepository;
import com.vamigo.ride.RideStop;
import com.vamigo.user.AccountStatus;
import com.vamigo.user.Role;
import com.vamigo.user.UserAccount;
import com.vamigo.user.UserAccountRepository;
import com.vamigo.user.UserProfile;
import com.vamigo.user.UserProfileRepository;
import com.vamigo.user.UserStats;
import com.vamigo.vehicle.Vehicle;
import com.vamigo.vehicle.VehicleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.vamigo.util.Constants.BIG_DECIMAL;
import static com.vamigo.util.Constants.FUTURE_DEPARTURE;
import static com.vamigo.util.Constants.ONE;
import static com.vamigo.util.Constants.RIDE_DESCRIPTION;
import static com.vamigo.util.TestFixtures.aDestinationLocation;
import static com.vamigo.util.TestFixtures.anOriginLocation;

/**
 * Persists composed aggregates into the real database. Backed by autowired repositories —
 * safe to call from {@code @Transactional} test methods.
 *
 * <p>Complements {@link TestFixtures} (which returns builders) by saving full graphs:
 * ride + stops, booking, user + profile, etc. Use {@link #issueJwt(UserAccount)} to obtain
 * a JWT for {@code Authorization: Bearer} headers.
 */
@Component
public class IntegrationFixtures {

    private static final AtomicLong EMAIL_SEQ = new AtomicLong(1);

    @Autowired private LocationRepository locationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private RideRepository rideRepository;
    @Autowired private RideBookingRepository bookingRepository;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @Transactional
    public Location persistLocation(Location.LocationBuilder builder) {
        Location loc = builder.id(null).build();
        return locationRepository.findByOsmId(loc.getOsmId())
                .orElseGet(() -> locationRepository.save(loc));
    }

    @Transactional
    public UserAccount persistUser(Role... roles) {
        String email = "user" + EMAIL_SEQ.getAndIncrement() + "@test.local";
        UserAccount acc = UserAccount.builder()
                .email(email)
                .passwordHash("{noop}pwd")
                .status(AccountStatus.ACTIVE)
                .roles(Set.of(roles.length == 0 ? Role.USER : roles[0]))
                .build();
        acc = userAccountRepository.save(acc);

        UserProfile profile = UserProfile.builder()
                .account(acc)
                .displayName("User " + acc.getId())
                .stats(new UserStats())
                .build();
        userProfileRepository.save(profile);
        return acc;
    }

    @Transactional
    public Vehicle persistVehicle(UserAccount owner) {
        Vehicle v = TestFixtures.aTesla()
                .id(null)
                .owner(owner)
                .build();
        return vehicleRepository.save(v);
    }

    /**
     * Persists a two-stop (origin, destination) ride for the given driver.
     * Defaults: autoApprove=true, totalSeats=1, future departure.
     */
    @Transactional
    public Ride persistSimpleRide(UserAccount driver) {
        Location origin = persistLocation(anOriginLocation());
        Location destination = persistLocation(aDestinationLocation());
        return persistRideWithStops(driver, 1, true, origin, destination);
    }

    /**
     * Persists a ride with arbitrary intermediate stops. stopOrder is 0..n-1.
     * First location is origin, last is destination.
     */
    @Transactional
    public Ride persistRideWithStops(UserAccount driver, int totalSeats, boolean autoApprove,
                                     Location... locations) {
        Vehicle vehicle = persistVehicle(driver);

        Ride ride = rideRepository.save(Ride.builder()
                .driver(driver)
                .vehicle(vehicle)
                .departureTime(FUTURE_DEPARTURE)
                .estimatedArrivalAt(FUTURE_DEPARTURE.plus(3, ChronoUnit.HOURS))
                .timePrecision(TimePrecision.EXACT)
                .totalSeats(totalSeats)
                .pricePerSeat(BIG_DECIMAL)
                .lastModified(Instant.now())
                .autoApprove(autoApprove)
                .description(RIDE_DESCRIPTION)
                .currency(Currency.PLN)
                .bookings(new ArrayList<>())
                .build());

        List<RideStop> stops = IntStream.range(0, locations.length)
                .mapToObj(i -> RideStop.builder()
                        .ride(ride)
                        .location(locations[i])
                        .stopOrder(i)
                        .departureTime(i == 0 ? FUTURE_DEPARTURE : null)
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
        ride.setStops(stops);
        return rideRepository.save(ride);
    }

    /**
     * Persists a booking. boardStop/alightStop default to first/last ride stops.
     */
    @Transactional
    public RideBooking persistBooking(Ride ride, UserAccount passenger, BookingStatus status) {
        RideStop board = ride.getStops().get(0);
        RideStop alight = ride.getStops().get(ride.getStops().size() - 1);
        return persistBooking(ride, passenger, board, alight, status, 1);
    }

    @Transactional
    public RideBooking persistBooking(Ride ride, UserAccount passenger,
                                      RideStop boardStop, RideStop alightStop,
                                      BookingStatus status, int seatCount) {
        RideBooking booking = RideBooking.builder()
                .ride(ride)
                .passenger(passenger)
                .boardStop(boardStop)
                .alightStop(alightStop)
                .status(status)
                .seatCount(seatCount)
                .bookedAt(Instant.now())
                .resolvedAt(status == BookingStatus.CONFIRMED ? Instant.now() : null)
                .proposedPrice(BigDecimal.valueOf(10))
                .build();
        return bookingRepository.save(booking);
    }

    public String issueJwt(UserAccount user) {
        return jwtTokenProvider.generateToken(user);
    }

    public String bearer(UserAccount user) {
        return "Bearer " + issueJwt(user);
    }

    public RideBookingRepository bookingRepository() { return bookingRepository; }

    public RideRepository rideRepository() { return rideRepository; }

    public UserAccountRepository userAccountRepository() { return userAccountRepository; }

    public UserProfileRepository userProfileRepository() { return userProfileRepository; }

    public LocationRepository locationRepository() { return locationRepository; }
}
