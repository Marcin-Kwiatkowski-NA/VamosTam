package com.blablatwo.messaging;

import com.blablatwo.messaging.dto.ConversationDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ConversationMapper {

    @Mapping(target = "rideId", source = "ride.id")
    @Mapping(target = "driverId", source = "driver.id")
    @Mapping(target = "driverName", source = "driver.name")
    @Mapping(target = "passengerId", source = "passenger.id")
    @Mapping(target = "passengerName", source = "passenger.name")
    @Mapping(target = "originName", source = "ride.origin.namePl")
    @Mapping(target = "destinationName", source = "ride.destination.namePl")
    @Mapping(target = "lastMessage", ignore = true)
    @Mapping(target = "unreadCount", ignore = true)
    ConversationDto toDto(Conversation conversation);
}
