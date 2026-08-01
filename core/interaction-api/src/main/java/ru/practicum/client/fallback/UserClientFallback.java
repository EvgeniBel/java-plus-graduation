package ru.practicum.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.client.UserClient;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class UserClientFallback implements UserClient {

    @Override
    public UserDto createUser(NewUserRequest userRequest) {
        log.warn("UserService недоступен при создании пользователя");
        return null;
    }

    @Override
    public UserDto getUserById(Long userId) {
        log.warn("UserService недоступен при получении пользователя ID: {}", userId);
        return createDefaultUserDto(userId);
    }

    @Override
    public List<UserDto> getUsers(Integer from, Integer size) {
        log.warn("UserService недоступен при получении списка пользователей");
        return new ArrayList<>();
    }

    @Override
    public List<UserDto> getUsersByIds(List<Long> userIds, Integer from, Integer size) {
        log.warn("UserService недоступен при получении пользователей по ID: {}", userIds);
        return userIds.stream()
                .map(this::createDefaultUserDto)
                .toList();
    }

    @Override
    public void deleteUser(Long userId) {
        log.warn("UserService недоступен при удалении пользователя ID: {}", userId);
    }

    @Override
    public boolean userExists(Long userId) {
        log.warn("UserService недоступен при проверке пользователя ID: {}, возвращаем true", userId);
        return true; // ⚠️ Возвращаем true, чтобы не блокировать выполнение
    }

    @Override
    public UserDto updateUser(Long userId, NewUserRequest userRequest) {
        log.warn("UserService недоступен при обновлении пользователя ID: {}", userId);
        return createDefaultUserDto(userId);
    }

    @Override
    public UserShortDto getUserShort(Long userId) {
        log.warn("UserService недоступен при получении краткой информации о пользователе ID: {}", userId);
        return createDefaultUserShortDto(userId);
    }

    @Override
    public List<UserShortDto> getUsersShort(Integer from, Integer size) {
        log.warn("UserService недоступен при получении списка пользователей (кратко)");
        return new ArrayList<>();
    }

    @Override
    public List<UserShortDto> searchUsersByName(String name, Integer from, Integer size) {
        log.warn("UserService недоступен при поиске пользователей по имени: {}", name);
        return new ArrayList<>();
    }

    @Override
    public boolean checkUserExists(Long userId) {
        log.warn("UserService недоступен при проверке пользователя (internal)");
        return true;
    }

    @Override
    public UserDto getUserInternal(Long userId) {
        log.warn("UserService недоступен при получении пользователя (internal)");
        return createDefaultUserDto(userId);
    }

    @Override
    public void validateUser(Long userId) {
        log.warn("UserService недоступен при валидации пользователя ID: {}", userId);
    }

    // ==================== DEFAULT МЕТОДЫ ====================

    private UserDto createDefaultUserDto(Long userId) {
        UserDto dto = new UserDto();
        dto.setId(userId);
        dto.setName("Unknown User");
        dto.setEmail("unknown@default.com");
        return dto;
    }

    private UserShortDto createDefaultUserShortDto(Long userId) {
        UserShortDto dto = new UserShortDto();
        dto.setId(userId);
        dto.setName("Unknown User");
        return dto;
    }
}