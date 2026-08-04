package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.UserDto;
import ru.practicum.service.UserService;

import static ru.practicum.constants.ApiConstants.USERS_INTERNAL;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(USERS_INTERNAL)
public class UserInternalController {

    private final UserService userService;

    @GetMapping("/{userId}/exists")
    public boolean checkUserExists(@PathVariable Long userId) {
        log.info("GET /internal/users/{}/exists", userId);
        return userService.existsUser(userId);
    }

    @GetMapping("/{userId}")
    public UserDto getUserInternal(@PathVariable Long userId) {
        log.info("GET /internal/users/{}", userId);
        return userService.getUserInternal(userId);
    }

    @PostMapping("/validate")
    public void validateUser(@RequestBody Long userId) {
        log.info("POST /internal/users/validate - userId={}", userId);
        userService.validateUser(userId);
    }
}