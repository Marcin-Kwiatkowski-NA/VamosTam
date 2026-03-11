package com.vamigo.ride;

import com.vamigo.ride.dto.BookingResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {RideStopMapper.class})
public abstract class BookingMapper {

    @Mapping(target = "rideId", expression = "java(booking.getRide().getId())")
    @Mapping(target = "boardStop", source = "boardStop")
    @Mapping(target = "alightStop", source = "alightStop")
    @Mapping(target = "passenger", ignore = true)
    @Mapping(target = "ride", ignore = true)
    @Mapping(target = "currency", expression = "java(booking.getRide().getCurrency())")
    public abstract BookingResponseDto toResponseDto(RideBooking booking);

    public abstract List<BookingResponseDto> toResponseDtos(List<RideBooking> bookings);
}
