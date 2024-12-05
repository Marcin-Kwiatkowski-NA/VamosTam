package com.blablatwo.city;

import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CityMapper {

    CityDTO toCityDTO(City city);
}
