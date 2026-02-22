package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.comment.dto.CommentFullDto;
import ru.practicum.comment.dto.CommentPublicDto;
import ru.practicum.comment.dto.NewCommentDto;
import ru.practicum.comment.dto.UpdCommentDto;
import ru.practicum.model.Comment;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    Comment toEntity(NewCommentDto newDto);

    @Mapping(target = "authorId", source = "author.id")
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "publishedOn", expression = "java(toLocalDateTime(comment.getPublishedOn()))")
    CommentFullDto toFullDto(Comment comment);

    @Mapping(target = "authorName", source = "author.name")
    @Mapping(target = "eventTitle", source = "event.title")
    @Mapping(target = "publishedOn", expression = "java(toLocalDateTime(comment.getPublishedOn()))")
    CommentPublicDto toPublicDto(Comment comment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "publishedOn", ignore = true)
    @Mapping(target = "state", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateFromDto(UpdCommentDto updDto, @MappingTarget Comment comment);

    default LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneOffset.UTC) : null;
    }
}