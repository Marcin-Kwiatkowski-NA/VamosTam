package com.blablatwo.city;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CityMapper {

    CityDTO toCityDTO(CityEntity cityEntity);
}
