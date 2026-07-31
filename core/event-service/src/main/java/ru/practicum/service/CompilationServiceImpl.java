package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.CreateCompilationDto;
import ru.practicum.dto.compilation.GetManyCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.model.Compilation;
import ru.practicum.repository.CompilationRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventService eventService;

    @Transactional
    @Override
    public CompilationDto createCompilation(CreateCompilationDto dto) {
        log.info("Создание подборки: title={}, events={}, pinned={}",
                dto.getTitle(), dto.getEvents(), dto.getPinned());

        Compilation compilation = CompilationMapper.toEntity(dto);
        Compilation saved = compilationRepository.save(compilation);
        log.info("Подборка создана с id: {}", saved.getId());

        // Получаем события для ответа
        List<Long> eventIds = CompilationMapper.getEventIdsFromString(saved.getEventIds());
        List<EventShortDto> events = eventService.getShortEventsInfoByIds(eventIds);

        return CompilationMapper.toCompilationDto(saved, events);
    }

    @Transactional
    @Override
    public CompilationDto updateCompilation(UpdateCompilationDto dto) {
        log.info("Обновление подборки с id: {}", dto.getId());

        Compilation compilation = compilationRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + dto.getId() + " не найдена"));

        if (dto.getTitle() != null) {
            compilation.setTitle(dto.getTitle());
        }
        if (dto.getPinned() != null) {
            compilation.setPinned(dto.getPinned());
        }
        if (dto.getEvents() != null) {
            compilation.setEventIds(convertListToString(dto.getEvents()));
        }

        Compilation updated = compilationRepository.save(compilation);
        log.info("Подборка обновлена, id: {}", updated.getId());

        List<Long> eventIds = CompilationMapper.getEventIdsFromString(updated.getEventIds());
        List<EventShortDto> events = eventService.getShortEventsInfoByIds(eventIds);

        return CompilationMapper.toCompilationDto(updated, events);
    }

    @Transactional
    @Override
    public void removeCompilation(Long compId) {
        log.info("Удаление подборки с id: {}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Подборка с id=" + compId + " не найдена");
        }
        compilationRepository.deleteById(compId);
        log.info("Подборка удалена, id: {}", compId);
    }

    @Override
    public List<CompilationDto> getCompilations(GetManyCompilationDto dto) {
        log.info("Получение подборок: pinned={}, from={}, size={}",
                dto.getPinned(), dto.getFrom(), dto.getSize());

        int page = dto.getFrom() / dto.getSize();
        Pageable pageable = PageRequest.of(page, dto.getSize());

        List<Compilation> compilations;
        if (dto.getPinned() != null) {
            compilations = compilationRepository.findAllByPinned(dto.getPinned(), pageable).getContent();
        } else {
            compilations = compilationRepository.findAll(pageable).getContent();
        }

        if (compilations.isEmpty()) {
            return new ArrayList<>();
        }

        // Собираем все ID событий из всех подборок
        List<Long> allEventIds = compilations.stream()
                .flatMap(c -> CompilationMapper.getEventIdsFromString(c.getEventIds()).stream())
                .distinct()
                .collect(Collectors.toList());

        // Получаем все события одной пачкой
        List<EventShortDto> allEvents = eventService.getShortEventsInfoByIds(allEventIds);
        Map<Long, EventShortDto> eventMap = allEvents.stream()
                .collect(Collectors.toMap(EventShortDto::getId, e -> e));

        // Формируем результат
        return compilations.stream()
                .map(comp -> {
                    List<Long> eventIds = CompilationMapper.getEventIdsFromString(comp.getEventIds());
                    List<EventShortDto> events = eventIds.stream()
                            .map(eventMap::get)
                            .filter(e -> e != null)
                            .collect(Collectors.toList());
                    return CompilationMapper.toCompilationDto(comp, events);
                })
                .collect(Collectors.toList());
    }

    @Override
    public CompilationDto getCompilationById(Long compId) {
        log.info("Получение подборки по id: {}", compId);

        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Подборка с id=" + compId + " не найдена"));

        List<Long> eventIds = CompilationMapper.getEventIdsFromString(compilation.getEventIds());
        List<EventShortDto> events = eventService.getShortEventsInfoByIds(eventIds);

        return CompilationMapper.toCompilationDto(compilation, events);
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================


    private String convertListToString(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return "";
        }
        return eventIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }
}