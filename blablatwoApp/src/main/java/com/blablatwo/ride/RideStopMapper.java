package com.blablatwo.ride;

import com.blablatwo.location.LocationMapper;
import com.blablatwo.ride.dto.RideStopDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", uses = {LocationMapper.class})
public abstract class RideStopMapper {

    @Autowired
    protected LocationMapper locationMapper;

    @Mapping(target = "location", expression = "java(locationMapper.locationToDto(stop.getLocation()))")
    public abstract RideStopDto rideStopToDto(RideStop stop);

    public abstract List<RideStopDto> rideStopsToDtos(List<RideStop> stops);
}
