package com.blablatwo.seat;

import com.blablatwo.city.CityMapper;
import com.blablatwo.seat.dto.SeatCreationDto;
import com.blablatwo.seat.dto.SeatResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {CityMapper.class})
public abstract class SeatMapper {

    @Autowired
    protected CityMapper cityMapper;

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passenger", ignore = true)
    @Mapping(target = "segment", ignore = true)
    @Mapping(target = "timeSlot", ignore = true)
    @Mapping(target = "lastModified", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "source", constant = "INTERNAL")
    @Mapping(target = "status", constant = "ACTIVE")
    public abstract Seat seatCreationDtoToEntity(SeatCreationDto dto);

    @Mapping(target = "origin", expression = "java(cityMapper.cityEntityToCityDto(seat.getSegment().getOrigin()))")
    @Mapping(target = "destination", expression = "java(cityMapper.cityEntityToCityDto(seat.getSegment().getDestination()))")
    @Mapping(target = "departureTime", expression = "java(seat.getTimeSlot().toLocalDateTime())")
    @Mapping(target = "isApproximate", expression = "java(seat.getTimeSlot().isApproximate())")
    @Mapping(target = "passenger", ignore = true)
    @Mapping(target = "contactMethods", ignore = true)
    @Mapping(target = "seatStatus", expression = "java(seat.computeSeatStatus())")
    public abstract SeatResponseDto seatEntityToResponseDto(Seat seat);
}
