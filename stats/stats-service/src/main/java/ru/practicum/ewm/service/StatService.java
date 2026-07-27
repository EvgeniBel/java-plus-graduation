package ru.practicum.ewm.service;

import ru.practicum.ewm.HitDto;
import ru.practicum.ewm.StatResponseDto;

import java.util.List;

public interface StatService {

    HitDto createHit(HitDto hitDto);

    List<StatResponseDto> getStats(String start, String end, List<String> uris, Boolean unique);
}