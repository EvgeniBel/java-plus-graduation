package ru.practicum;

import ru.practicum.ewm.HitDto;
import ru.practicum.ewm.StatRequestParamDto;
import ru.practicum.ewm.StatResponseDto;

import java.util.List;

public interface StatClient {

    HitDto postHit(HitDto dto);

    List<StatResponseDto> getStats(StatRequestParamDto dto);
}