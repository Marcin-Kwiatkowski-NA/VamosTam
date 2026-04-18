package com.vamigo.ride;

import tools.jackson.databind.ObjectMapper;
import com.vamigo.AbstractStompTest;
import com.vamigo.ride.dto.BookRideRequest;
import com.vamigo.ride.dto.BookingNotificationDto;
import com.vamigo.user.UserAccount;
import com.vamigo.util.IntegrationFixtures;
import com.vamigo.util.StompTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;

import static com.vamigo.util.Constants.OSM_ID_DESTINATION;
import static com.vamigo.util.Constants.OSM_ID_ORIGIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * STOMP round-trip over a real HTTP port. The passenger subscribes to their private
 * {@code /user/queue/bookings}; driver confirms a PENDING booking via real HTTP; the
 * {@link com.vamigo.ride.event.BookingBroadcastListener async after-commit listener} pushes
 * a {@link BookingNotificationDto} which the test drains from the subscription queue.
 */
// Suppress pending-booking expiry so the driver-confirm flow has time to run.
@TestPropertySource(properties = "booking.pending-ttl-minutes=60")
class BookingStompIT extends AbstractStompTest {

    @Autowired IntegrationFixtures fx;
    @Autowired ObjectMapper json;

    private RestTestClient http() {
        return RestTestClient.bindToServer()
                .baseUrl(baseUrl())
                .build();
    }

    @Test
    void confirmingPendingBooking_pushesNotificationOverStomp() throws Exception {
        UserAccount driver = fx.persistUser();
        UserAccount passenger = fx.persistUser();
        Ride ride = fx.persistRideWithStops(driver, 2, /* autoApprove */ false,
                fx.persistLocation(com.vamigo.util.TestFixtures.anOriginLocation()),
                fx.persistLocation(com.vamigo.util.TestFixtures.aDestinationLocation()));
        RideBooking booking = fx.persistBooking(ride, passenger, BookingStatus.PENDING);

        StompSession session = connectStomp(fx.issueJwt(passenger));
        BlockingQueue<BookingNotificationDto> queue =
                StompTestClient.subscribe(session, "/user/queue/bookings", BookingNotificationDto.class);

        // Driver confirms via real HTTP — servlet filter chain + security run for real.
        http().post()
                .uri("/rides/{rid}/bookings/{bid}/confirm", ride.getId(), booking.getId())
                .header("Authorization", fx.bearer(driver))
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk();

        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            BookingNotificationDto payload = queue.poll();
            assertThat(payload).isNotNull();
            assertThat(payload.bookingId()).isEqualTo(booking.getId());
            assertThat(payload.eventType()).isEqualTo("CONFIRMED");
        });

        session.disconnect();
    }

    @Test
    void connectWithInvalidJwt_failsToConnect() {
        assertThatThrownBy(() -> connectStomp("not-a-real-jwt"))
                .as("Invalid JWT must cause the CONNECT to be rejected by the interceptor")
                .isNotNull();
    }

    @Test
    void disconnectFromTestThread_closesSessionCleanly() throws Exception {
        UserAccount passenger = fx.persistUser();

        CompletableFuture<Boolean> disconnected = new CompletableFuture<>();
        StompSession session = connectStomp(fx.issueJwt(passenger), new StompSessionHandlerAdapter() {
            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                disconnected.complete(true);
            }
        });

        assertThat(session.isConnected()).isTrue();
        session.disconnect();

        await().atMost(Duration.ofSeconds(2)).until(() -> !session.isConnected());
    }

    // Silence unused-import warnings for types we reference via reflection/generics.
    @SuppressWarnings("unused")
    private void keepImports(StompHeaders h) { }
}
