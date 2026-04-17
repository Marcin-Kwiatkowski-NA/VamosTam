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
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PessimisticLockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.vamigo.util.TestFixtures.aDestinationLocation;
import static com.vamigo.util.TestFixtures.anActiveUserAccount;
import static com.vamigo.util.TestFixtures.anOriginLocation;
import static com.vamigo.util.TestFixtures.aRide;
import static com.vamigo.util.TestFixtures.aTesla;
import static com.vamigo.util.TestFixtures.aUserProfile;
import static com.vamigo.util.TestFixtures.buildStops;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class RideRepositoryLockIntegrationTest extends AbstractIntegrationTest {

    @Autowired private RideRepository rideRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private UserProfileRepository userProfileRepository;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private PlatformTransactionManager txManager;

    @PersistenceContext private EntityManager em;

    private Long rideId;
    private Long originId;
    private Long destinationId;
    private Long driverId;
    private Long vehicleId;

    @BeforeEach
    void setUp() {
        long nonce = System.nanoTime();
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Location origin = locationRepository.save(anOriginLocation().id(null)
                    .osmId(nonce).build());
            Location destination = locationRepository.save(aDestinationLocation().id(null)
                    .osmId(nonce + 1).build());
            UserAccount driver = userAccountRepository.save(anActiveUserAccount()
                    .email("lock-" + UUID.randomUUID() + "@example.com").build());
            userProfileRepository.save(aUserProfile(driver).build());
            Vehicle vehicle = vehicleRepository.save(aTesla().id(null)
                    .licensePlate("LK-" + nonce).owner(driver).build());

            Ride ride = aRide(origin, destination).id(null)
                    .driver(driver).vehicle(vehicle).status(Status.ACTIVE)
                    .stops(new ArrayList<>()).bookings(new ArrayList<>())
                    .build();
            ride.getStops().addAll(buildStops(ride, origin, destination));
            rideRepository.save(ride);

            rideId = ride.getId();
            originId = origin.getId();
            destinationId = destination.getId();
            driverId = driver.getId();
            vehicleId = vehicle.getId();
        });
    }

    @AfterEach
    void tearDown() {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            if (rideId != null) rideRepository.deleteById(rideId);
            if (vehicleId != null) vehicleRepository.deleteById(vehicleId);
            if (driverId != null) {
                userProfileRepository.deleteById(driverId);
                userAccountRepository.deleteById(driverId);
            }
            if (originId != null) locationRepository.deleteById(originId);
            if (destinationId != null) locationRepository.deleteById(destinationId);
        });
    }

    @Test
    void findByIdForUpdate_blocksSecondTransaction() throws InterruptedException {
        CountDownLatch firstHoldsLock = new CountDownLatch(1);
        CountDownLatch secondMayFinish = new CountDownLatch(1);
        AtomicReference<Throwable> secondResult = new AtomicReference<>();

        Thread holder = Thread.ofVirtual().start(() -> {
            try {
                new TransactionTemplate(txManager).executeWithoutResult(status -> {
                    try {
                        rideRepository.findByIdForUpdate(rideId).orElseThrow();
                    } finally {
                        firstHoldsLock.countDown();
                    }
                    try {
                        secondMayFinish.await(3, TimeUnit.SECONDS);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                });
            } finally {
                firstHoldsLock.countDown();
            }
        });

        Thread contender = Thread.ofVirtual().start(() -> {
            try {
                if (!firstHoldsLock.await(3, TimeUnit.SECONDS)) {
                    return;
                }
                new TransactionTemplate(txManager).executeWithoutResult(status -> {
                    em.createNativeQuery("SET LOCAL lock_timeout = '500ms'").executeUpdate();
                    rideRepository.findByIdForUpdate(rideId).orElseThrow();
                });
            } catch (Throwable t) {
                secondResult.set(t);
            } finally {
                secondMayFinish.countDown();
            }
        });

        holder.join();
        contender.join();

        assertThat(secondResult.get())
                .as("Contender must fail with a lock-contention exception")
                .isNotNull()
                .isInstanceOfAny(
                        CannotAcquireLockException.class,
                        PessimisticLockingFailureException.class,
                        org.hibernate.PessimisticLockException.class,
                        PessimisticLockException.class,
                        LockTimeoutException.class
                );
    }
}
