package com.vamigo.seat;

import com.vamigo.location.LocationMapper;
import com.vamigo.seat.dto.SeatCreationDto;
import com.vamigo.seat.dto.SeatResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {LocationMapper.class})
public abstract class SeatMapper {

    @Autowired
    protected LocationMapper locationMapper;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passenger", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "destination", ignore = true)
    @Mapping(target = "departureTime", ignore = true)
    @Mapping(target = "timePrecision", ignore = true)
    @Mapping(target = "lastModified", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "source", constant = "INTERNAL")
    @Mapping(target = "status", constant = "ACTIVE")
    public abstract Seat seatCreationDtoToEntity(SeatCreationDto dto);

    @Mapping(target = "origin", expression = "java(locationMapper.locationToDto(seat.getOrigin()))")
    @Mapping(target = "destination", expression = "java(locationMapper.locationToDto(seat.getDestination()))")
    @Mapping(target = "departureTime", source = "departureTime")
    @Mapping(target = "timePrecision", source = "timePrecision")
    @Mapping(target = "passenger", ignore = true)
    @Mapping(target = "contactMethods", ignore = true)
    @Mapping(target = "seatStatus", expression = "java(seat.computeSeatStatus())")
    public abstract SeatResponseDto seatEntityToResponseDto(Seat seat);
}
