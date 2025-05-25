package com.blablatwo.city;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CityMapper {

    CityResponseDto cityEntityToCityResponseDto(City city);

    @Mapping(target = "id", ignore = true)
    City cityCreationDtoToEntity(CityCreationDto cityDto);

    @Mapping(target = "id", ignore = true)
    void update(@MappingTarget City city, CityCreationDto cityDto);
}