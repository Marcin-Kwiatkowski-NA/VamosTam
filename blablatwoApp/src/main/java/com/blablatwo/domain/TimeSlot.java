package com.blablatwo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimeSlot {

    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @Column(name = "departure_time", nullable = false)
    private LocalTime departureTime;

    @Column(name = "is_approximate", nullable = false)
    private boolean isApproximate;

    public static TimeSlot of(LocalDateTime dateTime, boolean isApproximate) {
        return new TimeSlot(dateTime.toLocalDate(), dateTime.toLocalTime(), isApproximate);
    }

    public LocalDateTime toLocalDateTime() {
        return departureDate.atTime(departureTime);
    }
}
