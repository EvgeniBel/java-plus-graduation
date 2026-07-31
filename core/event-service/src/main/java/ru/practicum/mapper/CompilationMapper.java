package ru.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.CreateCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.model.Compilation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class CompilationMapper {

    public Compilation toEntity(CreateCompilationDto dto) {
        return Compilation.builder()
                .eventIds(convertListToString(dto.getEvents()))
                .pinned(dto.getPinned() != null ? dto.getPinned() : false)
                .title(dto.getTitle())
                .build();
    }


    public CompilationDto toCompilationDto(Compilation compilation, List<EventShortDto> events) {
        return CompilationDto.builder()
                .id(compilation.getId())
                .events(events != null ? events : new ArrayList<>())
                .pinned(compilation.getPinned())
                .title(compilation.getTitle())
                .build();
    }


    public UpdateCompilationDto toUpdateCompilationDto(Long compId, CreateCompilationDto dto) {
        return UpdateCompilationDto.builder()
                .id(compId)
                .events(dto.getEvents() != null ? dto.getEvents() : new ArrayList<>())
                .pinned(dto.getPinned())
                .title(dto.getTitle())
                .build();
    }


    public List<Long> getEventIdsFromString(String eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return new ArrayList<>();
        }
        String[] ids = eventIds.split(",");
        List<Long> result = new ArrayList<>();
        for (String id : ids) {
            try {
                result.add(Long.parseLong(id.trim()));
            } catch (NumberFormatException ignored) {
                // Игнорируем невалидные ID
            }
        }
        return result;
    }


    private String convertListToString(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return "";
        }
        return eventIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}
