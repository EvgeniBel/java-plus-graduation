package ru.practicum.aggregator.service;

import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;

import java.util.List;

public interface UserService {

    UserDto createUser(NewUserRequest request);

    List<UserDto> getUsers(List<Long> ids, Integer from, Integer size);

    void deleteUser(Long userId);

    UserShortDto getUserShort(Long userId);

    List<UserShortDto> getUsersShort(Integer from, Integer size);

    List<UserShortDto> searchUsersByName(String name, Integer from, Integer size);

    boolean existsUser(Long userId);

    UserDto getUserInternal(Long userId);

    void validateUser(Long userId);
}