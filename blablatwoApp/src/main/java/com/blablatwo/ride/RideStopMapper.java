package com.blablatwo.ride;

import com.blablatwo.city.CityMapper;
import com.blablatwo.ride.dto.RideStopDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Mapper(componentModel = "spring", uses = {CityMapper.class})
public abstract class RideStopMapper {

    @Autowired
    protected CityMapper cityMapper;

    @Mapping(target = "city", expression = "java(cityMapper.cityEntityToCityDto(stop.getCity()))")
    public abstract RideStopDto rideStopToDto(RideStop stop);

    public abstract List<RideStopDto> rideStopsToDtos(List<RideStop> stops);
}
