package ru.practicum.client;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.client.fallback.UserClientFallback;
import ru.practicum.aggregator.collector.config.FeignConfig;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;

import java.util.List;

import static ru.practicum.constants.ApiConstants.*;

@FeignClient(
        name = "user-service",
        fallback = UserClientFallback.class,
        configuration = FeignConfig.class
)
public interface UserClient {

    @PostMapping(USERS_BASE)
    UserDto createUser(@Valid @RequestBody NewUserRequest userRequest);

    @GetMapping(USER_BY_ID)
    UserDto getUserById(@PathVariable(USER_BY_ID_PARAM) Long userId);

    @GetMapping(USERS_BASE)
    List<UserDto> getUsers(
            @RequestParam(value = FROM_PARAM, defaultValue = DEFAULT_FROM) Integer from,
            @RequestParam(value = SIZE_PARAM, defaultValue = DEFAULT_SIZE) Integer size
    );

    @GetMapping(USERS_BY_IDS)
    List<UserDto> getUsersByIds(
            @RequestParam("ids") List<Long> userIds,
            @RequestParam(value = FROM_PARAM, defaultValue = DEFAULT_FROM) Integer from,
            @RequestParam(value = SIZE_PARAM, defaultValue = DEFAULT_SIZE) Integer size
    );

    @DeleteMapping(USER_BY_ID)
    void deleteUser(@PathVariable(USER_BY_ID_PARAM) Long userId);

    @GetMapping(USER_EXISTS)
    boolean userExists(@PathVariable(USER_BY_ID_PARAM) Long userId);

    @PutMapping(USER_BY_ID)
    UserDto updateUser(
            @PathVariable(USER_BY_ID_PARAM) Long userId,
            @Valid @RequestBody NewUserRequest userRequest
    );

    @GetMapping(USER_SHORT_PUBLIC)
    UserShortDto getUserShort(@PathVariable(USER_BY_ID_PARAM) Long userId);

    @GetMapping(USER_SHORT_PUBLIC)
    List<UserShortDto> getUsersShort(
            @RequestParam(value = FROM_PARAM, defaultValue = DEFAULT_FROM) Integer from,
            @RequestParam(value = SIZE_PARAM, defaultValue = DEFAULT_SIZE) Integer size
    );

    @GetMapping(USERS_SEARCH_PUBLIC)
    List<UserShortDto> searchUsersByName(
            @RequestParam("name") String name,
            @RequestParam(value = FROM_PARAM, defaultValue = DEFAULT_FROM) Integer from,
            @RequestParam(value = SIZE_PARAM, defaultValue = DEFAULT_SIZE) Integer size
    );

    @GetMapping(USER_EXISTS_INTERNAL)
    boolean checkUserExists(@PathVariable(USER_BY_ID_PARAM) Long userId);

    @GetMapping(USER_BY_ID_INTERNAL)
    UserDto getUserInternal(@PathVariable(USER_BY_ID_PARAM) Long userId);

    @PostMapping(USER_VALIDATE_INTERNAL)
    void validateUser(@RequestBody Long userId);
}