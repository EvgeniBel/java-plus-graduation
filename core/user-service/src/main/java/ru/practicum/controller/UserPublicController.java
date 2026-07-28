package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.service.UserService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/users")
public class UserPublicController {

    private final UserService userService;

    @GetMapping("/{userId}/short")
    public UserShortDto getUserShort(@PathVariable Long userId) {
        log.info("GET /public/users/{}/short", userId);
        return userService.getUserShort(userId);
    }

    @GetMapping("/short")
    public List<UserShortDto> getUsersShort(
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("GET /public/users/short?from={}&size={}", from, size);
        return userService.getUsersShort(from, size);
    }

    @GetMapping("/search")
    public List<UserShortDto> searchUsersByName(
            @RequestParam String name,
            @RequestParam(defaultValue = "0") Integer from,
            @RequestParam(defaultValue = "10") Integer size) {
        log.info("GET /public/users/search?name={}&from={}&size={}", name, from, size);
        return userService.searchUsersByName(name, from, size);
    }
}