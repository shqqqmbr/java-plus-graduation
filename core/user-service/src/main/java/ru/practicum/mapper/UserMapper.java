package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.model.User;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;
import ru.practicum.user.dto.UserShortDto;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDto toFullDto(User user);

    @Mapping(target = "id", ignore = true)
    User toEntity(NewUserRequest newDto);

    UserShortDto toShortDto(User user);
}