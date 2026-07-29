package ru.practicum.service;


import ru.practicum.dto.request.CreateUpdateRequestDto;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    //    Создание нового запроса
    ParticipationRequestDto createRequest(Long userId, CreateUpdateRequestDto dto);

    //    Получение всех запросов определённого пользователя
    List<ParticipationRequestDto> getRequestByUserId(Long userId);

    //    Отмена запроса на событие
    ParticipationRequestDto canceledRequest(Long userId, Long requestId);

}
