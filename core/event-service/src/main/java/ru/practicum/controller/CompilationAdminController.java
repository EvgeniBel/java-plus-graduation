package ru.practicum.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.CreateCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationDto;
import ru.practicum.service.CompilationService;

import static ru.practicum.constants.ApiConstants.COMPILATIONS_BASE;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(COMPILATIONS_BASE)
public class CompilationAdminController {

    private final CompilationService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CompilationDto createCompilation(@Valid @RequestBody CreateCompilationDto dto) {
        log.info("POST /admin/compilations - создание подборки: title={}, events={}, pinned={}",
                dto.getTitle(), dto.getEvents(), dto.getPinned());

        if (dto.getPinned() == null) {
            dto.setPinned(false);
        }
        return service.createCompilation(dto);
    }

    @PatchMapping("/{compId}")
    public CompilationDto updateCompilation(
            @PositiveOrZero @PathVariable Long compId,
            @Valid @RequestBody UpdateCompilationDto dto
    ) {
        log.info("PATCH /admin/compilations/{} - обновление подборки: events={}, pinned={}, title={}",
                compId, dto.getEvents(), dto.getPinned(), dto.getTitle());

        // Устанавливаем ID для сервиса
        dto.setId(compId);
        return service.updateCompilation(dto);
    }

    @DeleteMapping("/{compId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeCompilation(@PositiveOrZero @PathVariable Long compId) {
        log.info("DELETE /admin/compilations/{}", compId);
        service.removeCompilation(compId);
    }
}