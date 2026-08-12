package ru.practicum.aggregator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
import ru.practicum.aggregator.collector.mapper.CompilationMapper;
import ru.practicum.aggregator.model.Compilation;
import ru.practicum.aggregator.model.Event;
import ru.practicum.aggregator.repository.CompilationRepository;
import ru.practicum.aggregator.repository.EventRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final EventService eventService;

    @Transactional
    @Override
    public CompilationDto createCompilation(CreateCompilationDto dto) {
        log.info("Создание подборки: title={}, events={}, pinned={}",
                dto.getTitle(), dto.getEvents(), dto.getPinned());

        Compilation compilation = CompilationMapper.toEntity(dto);

        // Если есть события - загружаем их и добавляем в подборку
        if (dto.getEvents() != null && !dto.getEvents().isEmpty()) {
            List<Event> events = eventRepository.findAllById(dto.getEvents());
            compilation.setEvents(events);
        }

        Compilation saved = compilationRepository.save(compilation);
        log.info("Подборка создана с id: {}", saved.getId());

        List<EventShortDto> events = eventService.getShortEventsInfoByIds(
                CompilationMapper.getEventIds(saved)
        );

        return CompilationMapper.toCompilationDto(saved, events);
    }

    @Transactional
    @Override
    public CompilationDto updateCompilation(UpdateCompilationDto dto) {
        log.info("Обновление подборки с id: {}", dto.getId());

        Compilation compilation = compilationRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException(String.format("Подборка с id=%s не найдена", dto.getId())));

        CompilationMapper.updateEntity(compilation, dto);

        if (dto.getEvents() != null) {
            List<Event> events = eventRepository.findAllById(dto.getEvents());
            compilation.setEvents(events);
        }

        Compilation updated = compilationRepository.save(compilation);
        log.info("Подборка обновлена, id: {}", updated.getId());

        List<EventShortDto> events = eventService.getShortEventsInfoByIds(
                CompilationMapper.getEventIds(updated)
        );

        return CompilationMapper.toCompilationDto(updated, events);
    }

    @Transactional
    @Override
    public void removeCompilation(Long compId) {
        log.info("Удаление подборки с id: {}", compId);

        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException(String.format("Подборка с id=%s не найдена", compId));
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

        Page<Compilation> compilationsPage;
        if (dto.getPinned() != null) {
            compilationsPage = compilationRepository.findAllByPinned(dto.getPinned(), pageable);
        } else {
            compilationsPage = compilationRepository.findAll(pageable);
        }

        List<Compilation> compilations = compilationsPage.getContent();

        if (compilations.isEmpty()) {
            return new ArrayList<>();
        }

        List<Long> allEventIds = compilations.stream()
                .flatMap(c -> CompilationMapper.getEventIds(c).stream())
                .distinct()
                .collect(Collectors.toList());

        // Получаем все события одной пачкой
        List<EventShortDto> allEvents = eventService.getShortEventsInfoByIds(allEventIds);
        Map<Long, EventShortDto> eventMap = allEvents.stream()
                .collect(Collectors.toMap(EventShortDto::getId, e -> e));

        return compilations.stream()
                .map(comp -> {
                    List<Long> eventIds = CompilationMapper.getEventIds(comp);
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

        Compilation compilation = compilationRepository.findByIdWithEvents(compId);
        if (compilation == null) {
            throw new NotFoundException(String.format("Подборка с id=%s не найдена", compId));
        }

        List<Long> eventIds = CompilationMapper.getEventIds(compilation);
        List<EventShortDto> events = eventService.getShortEventsInfoByIds(eventIds);

        return CompilationMapper.toCompilationDto(compilation, events);
    }
}