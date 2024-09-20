package com.blablatwo.ride.DTO;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RideCreationDto(Long vehicleId,
                              Long originCityId,
                              Long destinationCityId,
                              LocalDateTime departureTime,
                              int availableSeats,
                              @DecimalMin("0.0") BigDecimal pricePerSeat,
                              List<Long> stopCityIds) {
}
