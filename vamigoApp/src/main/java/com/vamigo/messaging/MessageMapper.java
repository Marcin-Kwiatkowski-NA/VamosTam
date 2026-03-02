package com.vamigo.messaging;

import com.vamigo.messaging.dto.MessageDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MessageMapper {

    @Mapping(target = "conversationId", source = "conversation.id")
    @Mapping(target = "senderId", source = "sender.id")
    @Mapping(target = "isMine", ignore = true)
    @Mapping(target = "status", expression = "java(message.getDerivedStatus())")
    MessageDto toDto(Message message);

    List<MessageDto> toDtoList(List<Message> messages);
}
