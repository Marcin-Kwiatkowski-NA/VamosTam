package com.blablatwo.ride;

import com.blablatwo.ride.dto.BookingResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {RideStopMapper.class})
public abstract class BookingMapper {

    @Mapping(target = "rideId", expression = "java(booking.getRide().getId())")
    @Mapping(target = "boardStop", source = "boardStop")
    @Mapping(target = "alightStop", source = "alightStop")
    @Mapping(target = "passenger", ignore = true)
    public abstract BookingResponseDto toResponseDto(RideBooking booking);

    public abstract List<BookingResponseDto> toResponseDtos(List<RideBooking> bookings);
}
