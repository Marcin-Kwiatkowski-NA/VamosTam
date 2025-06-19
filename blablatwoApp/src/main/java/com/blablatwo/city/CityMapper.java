package com.blablatwo.city;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CityMapper {

    CityDto cityEntityToCityDto(City city);

    @Mapping(target = "id", ignore = true)
    City cityDtoToEntity(CityDto cityDto);

    @Mapping(target = "id", ignore = true)
    void update(@MappingTarget City city, CityDto cityDto);
}