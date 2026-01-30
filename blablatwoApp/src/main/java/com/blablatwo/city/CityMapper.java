package com.blablatwo.city;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CityMapper {

    /**
     * Convert City entity to CityDto with default language (Polish).
     */
    @Mapping(target = "name", expression = "java(city.getDisplayName(\"pl\"))")
    CityDto cityEntityToCityDto(City city);

    /**
     * Convert City entity to CityDto with specified language.
     *
     * @param city The city entity
     * @param lang Language code ("pl" or "en")
     */
    @Mapping(target = "name", expression = "java(city.getDisplayName(lang))")
    CityDto cityEntityToCityDto(City city, @Context String lang);
}
