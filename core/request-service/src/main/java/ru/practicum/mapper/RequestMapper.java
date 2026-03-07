package ru.practicum.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.model.Request;
import ru.practicum.request.dto.ParticipationRequestDto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    @Mapping(target = "event", source = "eventId")
    @Mapping(target = "requester", source = "requesterId")
    @Mapping(target = "created", expression = "java(toLocalDateTime(request.getCreated()))")
    ParticipationRequestDto toDto(Request request);

    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneOffset.UTC) : null;
    }
}