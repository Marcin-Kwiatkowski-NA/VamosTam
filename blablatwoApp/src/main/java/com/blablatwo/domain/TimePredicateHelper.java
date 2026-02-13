package com.blablatwo.domain;

import jakarta.persistence.criteria.Path;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;

public final class TimePredicateHelper {

    private TimePredicateHelper() {
    }

    /**
     * Filter entities departing on or after the given date and time.
     * Logic: (departure_date > date) OR (departure_date = date AND departure_time >= time)
     *
     * @param time must be non-null; callers are responsible for providing a concrete value
     */
    public static <T> Specification<T> departsOnOrAfter(LocalDate date, LocalTime time) {
        return (root, query, cb) -> {
            if (date == null) return null;
            Path<LocalDate> datePath = root.get("departureDate");
            Path<LocalTime> timePath = root.get("departureTime");
            return cb.or(
                    cb.greaterThan(datePath, date),
                    cb.and(
                            cb.equal(datePath, date),
                            cb.greaterThanOrEqualTo(timePath, time)
                    )
            );
        };
    }

    /**
     * Filter entities departing on or before the given date and time.
     * Logic: (departure_date < date) OR (departure_date = date AND departure_time <= time)
     *
     * @param time must be non-null; callers are responsible for providing a concrete value
     */
    public static <T> Specification<T> departsOnOrBefore(LocalDate date, LocalTime time) {
        return (root, query, cb) -> {
            if (date == null) return null;
            Path<LocalDate> datePath = root.get("departureDate");
            Path<LocalTime> timePath = root.get("departureTime");
            return cb.or(
                    cb.lessThan(datePath, date),
                    cb.and(
                            cb.equal(datePath, date),
                            cb.lessThanOrEqualTo(timePath, time)
                    )
            );
        };
    }

    /**
     * Shared "from boundary" calculation — ensures Ride and Seat searches never diverge.
     * <ul>
     *   <li>No departureDate → today + now</li>
     *   <li>departureDate with departureTimeFrom → use both</li>
     *   <li>departureDate == today, no time → today + now</li>
     *   <li>departureDate in future, no time → that date + midnight</li>
     * </ul>
     */
    public record DepartureFrom(LocalDate date, LocalTime time) {
    }

    public static DepartureFrom calculateDepartureFrom(LocalDate departureDate, LocalTime departureTimeFrom) {
        if (departureDate == null) {
            return new DepartureFrom(LocalDate.now(), LocalTime.now());
        }
        if (departureTimeFrom != null) {
            return new DepartureFrom(departureDate, departureTimeFrom);
        }
        if (departureDate.equals(LocalDate.now())) {
            return new DepartureFrom(LocalDate.now(), LocalTime.now());
        }
        return new DepartureFrom(departureDate, LocalTime.MIN);
    }
}
