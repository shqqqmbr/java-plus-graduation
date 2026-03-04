package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;
import ru.practicum.user.dto.NewUserRequest;
import ru.practicum.user.dto.UserDto;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto add(NewUserRequest newDto) {
        if (userRepository.existsByEmail(newDto.getEmail())) {
            throw new ConflictException("User с Email={} уже существует", newDto.getEmail());
        }

        String localpart = newDto.getEmail().substring(0, newDto.getEmail().indexOf('@'));
        if (localpart.length() > 64) {
            throw new BadRequestException("Localpart is too long");
        }
        User savedUser = userRepository.save(userMapper.toEntity(newDto));
        return userMapper.toFullDto(savedUser);
    }

    @Override
    public List<UserDto> findAllBy(List<Long> ids, Integer from, Integer size) {
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);
        List<User> users;

        if (ids == null || ids.isEmpty()) {
            users = userRepository.findAll(pageable).getContent();
        } else {
            users = userRepository.findAllByIdIn(ids, pageable);
        }

        return users.stream()
                .map(userMapper::toFullDto)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
        } else {
            throw new NotFoundException("User userId={} не найден", userId);
        }
    }

    @Override
    public UserDto getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User userId={} не найден", userId));
        return userMapper.toFullDto(user);
    }

    @Override
    public List<UserDto> getUsersByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<User> users = userRepository.findAllById(ids);
        return users.stream()
                .map(userMapper::toFullDto)
                .toList();
    }
}