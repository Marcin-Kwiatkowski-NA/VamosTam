package com.vamigo.util;

import com.vamigo.ride.event.BookingCancelledEvent;
import com.vamigo.ride.event.BookingConfirmedEvent;
import com.vamigo.ride.event.BookingExpiredEvent;
import com.vamigo.ride.event.BookingRejectedEvent;
import com.vamigo.ride.event.BookingRequestedEvent;
import com.vamigo.ride.event.RideCompletedEvent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Application-scoped bean that records domain events outside the test's transaction.
 *
 * <p>{@code @RecordApplicationEvents} only captures events published inside the test's transaction.
 * {@code @TransactionalEventListener(AFTER_COMMIT)} and async/after-commit listeners fire outside
 * the test transaction — any secondary events they publish are invisible to {@code ApplicationEvents}.
 *
 * <p>This collector runs in the normal application context via plain {@code @EventListener}
 * (no {@code @Transactional}), so it sees everything that crosses the publisher. Tests activate it
 * with {@code @Import(TestEventCollector.class)}.
 */
@TestConfiguration(proxyBeanMethods = false)
@Lazy
public class TestEventCollector {

    private final List<BookingRequestedEvent> bookingRequested = new CopyOnWriteArrayList<>();
    private final List<BookingConfirmedEvent> bookingConfirmed = new CopyOnWriteArrayList<>();
    private final List<BookingRejectedEvent> bookingRejected = new CopyOnWriteArrayList<>();
    private final List<BookingCancelledEvent> bookingCancelled = new CopyOnWriteArrayList<>();
    private final List<BookingExpiredEvent> bookingExpired = new CopyOnWriteArrayList<>();
    private final List<RideCompletedEvent> rideCompleted = new CopyOnWriteArrayList<>();

    @EventListener
    public void onBookingRequested(BookingRequestedEvent e) { bookingRequested.add(e); }

    @EventListener
    public void onBookingConfirmed(BookingConfirmedEvent e) { bookingConfirmed.add(e); }

    @EventListener
    public void onBookingRejected(BookingRejectedEvent e) { bookingRejected.add(e); }

    @EventListener
    public void onBookingCancelled(BookingCancelledEvent e) { bookingCancelled.add(e); }

    @EventListener
    public void onBookingExpired(BookingExpiredEvent e) { bookingExpired.add(e); }

    @EventListener
    public void onRideCompleted(RideCompletedEvent e) { rideCompleted.add(e); }

    public List<BookingRequestedEvent> bookingRequested() { return List.copyOf(bookingRequested); }
    public List<BookingConfirmedEvent> bookingConfirmed() { return List.copyOf(bookingConfirmed); }
    public List<BookingRejectedEvent> bookingRejected() { return List.copyOf(bookingRejected); }
    public List<BookingCancelledEvent> bookingCancelled() { return List.copyOf(bookingCancelled); }
    public List<BookingExpiredEvent> bookingExpired() { return List.copyOf(bookingExpired); }
    public List<RideCompletedEvent> rideCompleted() { return List.copyOf(rideCompleted); }

    public void clear() {
        bookingRequested.clear();
        bookingConfirmed.clear();
        bookingRejected.clear();
        bookingCancelled.clear();
        bookingExpired.clear();
        rideCompleted.clear();
    }
}
