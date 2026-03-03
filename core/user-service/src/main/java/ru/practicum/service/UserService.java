package ru.practicum.service;

import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.util.List;

public interface UserService {

    List<UserDto> findAllBy(List<Long> ids, Integer from, Integer size);

    UserDto add(NewUserRequest newDto);

    void delete(Long userId);

    UserDto getUserById(Long userId);

    List<UserDto> getUsersByIds(List<Long> ids);
}