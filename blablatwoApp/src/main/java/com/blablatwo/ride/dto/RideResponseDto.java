package com.blablatwo.ride.dto;

import com.blablatwo.city.CityDTO;
import com.blablatwo.traveler.DriverProfileDto;
import com.blablatwo.vehicle.VehicleResponseDTO;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record RideResponseDto(Long id,
                              DriverProfileDto driver,
                              VehicleResponseDTO vehicle,
                              CityDTO origin,
                              CityDTO destination,
                              List<CityDTO> stops,
                              LocalDateTime departureTime,
                              int availableSeats,
                              BigDecimal pricePerSeat,
                              String rideStatus,
                              Instant lastModified,
                              List<DriverProfileDto> passengers) {
}
