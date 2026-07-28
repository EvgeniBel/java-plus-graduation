package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.UserMapper;
import ru.practicum.model.User;
import ru.practicum.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto createUser(NewUserRequest request) {
        log.info("Создание пользователя: {}", request);

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException(String.format("Пользователь с email '%s' уже существует", request.getEmail()));
        }

        User user = UserMapper.toEntity(request);
        User savedUser = userRepository.save(user);
        log.info("Пользователь создан с id: {}", savedUser.getId());
        return UserMapper.toUserDto(savedUser);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, Integer from, Integer size) {
        log.info("Получение пользователей: ids={}, from={}, size={}", ids, from, size);
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAllByIds(ids, pageable)
                .stream()
                .map(UserMapper::toUserDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        log.info("Удаление пользователя с id: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("Пользователь с id=%s не найден", userId));
        }

        userRepository.deleteById(userId);
        log.info("Пользователь с id: {} удален", userId);
    }

    // ==================== PUBLIC METHODS ====================

    @Override
    @Transactional(readOnly = true)
    public UserShortDto getUserShort(Long userId) {
        log.info("Получение краткой информации о пользователе: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id=%s не найден", userId)));
        return UserMapper.toUserShortDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserShortDto> getUsersShort(Integer from, Integer size) {
        log.info("Получение краткой информации о пользователях: from={}, size={}", from, size);
        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable)
                .stream()
                .map(UserMapper::toUserShortDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserShortDto> searchUsersByName(String name, Integer from, Integer size) {
        log.info("Поиск пользователей по имени: name={}, from={}, size={}", name, from, size);

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findAll(pageable)
                .stream()
                .map(UserMapper::toUserShortDto)
                .collect(Collectors.toList());
    }

    // ==================== INTERNAL METHODS ====================

    @Override
    @Transactional(readOnly = true)
    public boolean existsUser(Long userId) {
        log.info("Проверка существования пользователя: {}", userId);
        return userRepository.existsById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto getUserInternal(Long userId) {
        log.info("Внутреннее получение пользователя: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id=%s не найден", userId)));
        return UserMapper.toUserDto(user);
    }

    @Override
    @Transactional(readOnly = true)
    public void validateUser(Long userId) {
        log.info("Валидация пользователя: {}", userId);
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException(String.format("Пользователь с id=%s не найден", userId));
        }
    }
}