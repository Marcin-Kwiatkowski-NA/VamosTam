package com.vamigo.seat;

import com.vamigo.location.Location;
import com.vamigo.location.LocationMapper;
import com.vamigo.seat.dto.SeatCreationDto;
import com.vamigo.seat.dto.SeatListDto;
import com.vamigo.seat.dto.SeatResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {LocationMapper.class})
public abstract class SeatMapper {

    @Autowired
    protected LocationMapper locationMapper;

    public SeatDetails toDetails(SeatCreationDto dto, Location origin, Location destination) {
        return new SeatDetails(
                origin,
                destination,
                dto.departureTime(),
                dto.timePrecision(),
                dto.count(),
                dto.priceWillingToPay(),
                dto.description(),
                dto.contactPhone(),
                dto.currency()
        );
    }

    @Mapping(target = "origin", expression = "java(locationMapper.locationToDto(seat.getOrigin()))")
    @Mapping(target = "destination", expression = "java(locationMapper.locationToDto(seat.getDestination()))")
    @Mapping(target = "departureTime", source = "departureTime")
    @Mapping(target = "timePrecision", source = "timePrecision")
    @Mapping(target = "passenger", ignore = true)
    @Mapping(target = "contactMethods", ignore = true)
    @Mapping(target = "seatStatus", expression = "java(seat.computeSeatStatus())")
    public abstract SeatResponseDto seatEntityToResponseDto(Seat seat);

    @Mapping(target = "origin", expression = "java(locationMapper.locationToDto(seat.getOrigin()))")
    @Mapping(target = "destination", expression = "java(locationMapper.locationToDto(seat.getDestination()))")
    @Mapping(target = "passenger", ignore = true)
    @Mapping(target = "seatStatus", expression = "java(seat.computeSeatStatus())")
    public abstract SeatListDto seatEntityToSeatListDto(Seat seat);
}
