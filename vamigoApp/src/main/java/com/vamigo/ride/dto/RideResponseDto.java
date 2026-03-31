package com.vamigo.ride.dto;

import com.vamigo.domain.Currency;
import com.vamigo.dto.ContactMethodDto;
import com.vamigo.dto.UserCardDto;
import com.vamigo.location.LocationDto;
import com.vamigo.ride.RideSource;
import com.vamigo.ride.RideStatus;
import com.vamigo.vehicle.VehicleResponseDto;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Builder(toBuilder = true)
public record RideResponseDto(
        Long id,
        RideSource source,
        LocationDto origin,
        LocationDto destination,
        List<RideStopDto> stops,
        Instant departureTime,
        boolean isTimeApproximate,
        BigDecimal pricePerSeat,
        int availableSeats,
        int seatsTaken,
        int totalSeats,
        boolean autoApprove,
        boolean doorToDoor,
        String description,
        String contactPhone,
        UserCardDto driver,
        List<ContactMethodDto> contactMethods,
        VehicleResponseDto vehicle,
        RideStatus rideStatus,
        List<BookingResponseDto> bookings,
        Currency currency
) {
}
