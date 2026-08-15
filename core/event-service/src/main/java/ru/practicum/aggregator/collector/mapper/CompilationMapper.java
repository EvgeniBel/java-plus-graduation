package ru.practicum.aggregator.collector.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.aggregator.model.Compilation;
import ru.practicum.aggregator.model.Event;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.CreateCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationDto;
import ru.practicum.dto.event.EventShortDto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@UtilityClass
public class CompilationMapper {

    public Compilation toEntity(CreateCompilationDto dto) {
        return Compilation.builder()
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

    public void updateEntity(Compilation compilation, UpdateCompilationDto dto) {
        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }
        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }
    }

    public List<Long> getEventIds(Compilation compilation) {
        if (compilation.getEvents() == null || compilation.getEvents().isEmpty()) {
            return new ArrayList<>();
        }
        return compilation.getEvents().stream()
                .map(Event::getId)
                .collect(Collectors.toList());
    }

    public void setEvents(Compilation compilation, List<Event> events) {
        if (events == null) {
            compilation.setEvents(new ArrayList<>());
        } else {
            compilation.setEvents(events);
        }
    }
}