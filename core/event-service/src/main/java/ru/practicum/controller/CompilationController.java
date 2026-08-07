package ru.practicum.controller;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.GetManyCompilationDto;
import ru.practicum.aggregator.service.CompilationService;

import java.util.List;

import static ru.practicum.constants.ApiConstants.COMPILATION_ID_PATH;
import static ru.practicum.constants.ApiConstants.COMPILATION_PREFIX;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(COMPILATION_PREFIX)
public class CompilationController {

    private final CompilationService service;

    @GetMapping
    public List<CompilationDto> getCompilations(
            @RequestParam(required = false) Boolean pinned,
            @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
            @PositiveOrZero @RequestParam(defaultValue = "10") Integer size
    ) {
        log.info("GET /compilations - pinned={}, from={}, size={}", pinned, from, size);

        GetManyCompilationDto dto = GetManyCompilationDto.builder()
                .pinned(pinned)
                .from(from)
                .size(size)
                .build();

        return service.getCompilations(dto);
    }

    @GetMapping(COMPILATION_ID_PATH)
    public CompilationDto getCompilationById(@PathVariable Long compId) {
        log.info("GET /compilations/{}", compId);
        return service.getCompilationById(compId);
    }
}
