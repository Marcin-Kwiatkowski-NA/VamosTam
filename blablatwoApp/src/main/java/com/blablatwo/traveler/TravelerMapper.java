package com.blablatwo.traveler;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TravelerMapper {

    @Mapping(target = "pictureUrl", source = "googleUser.pictureUrl")
    TravelerResponseDto travelerEntityToTravelerResponseDto(Traveler traveler);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", constant = "1")
    @Mapping(target = "authority", defaultValue = "ROLE_PASSENGER")
    @Mapping(target = "type", defaultValue = "PASSENGER")
    @Mapping(target = "ridesAsDriver", ignore = true)
    @Mapping(target = "ridesAsPassenger", ignore = true)
    @Mapping(target = "vehicles", ignore = true)
    @Mapping(target = "googleUser", ignore = true)
    @Mapping(target = "version", ignore = true)
    Traveler travelerCreationDtoToEntity(TravelerCreationDto travelerDto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "username", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    @Mapping(target = "authority", ignore = true)
    @Mapping(target = "ridesAsDriver", ignore = true)
    @Mapping(target = "ridesAsPassenger", ignore = true)
    @Mapping(target = "vehicles", ignore = true)
    @Mapping(target = "googleUser", ignore = true)
    @Mapping(target = "version", ignore = true)
    void update(@MappingTarget Traveler traveler, TravelerCreationDto travelerDto);
}