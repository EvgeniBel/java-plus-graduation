package ru.practicum.client.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.client.RequestClient;
import ru.practicum.dto.request.CreateUpdateRequestDto;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class RequestClientFallback implements RequestClient {

    @Override
    public Long getConfirmedRequestsCount(Long eventId) {
        log.warn("RequestService недоступен при получении количества запросов для события ID: {}, возвращаем 0", eventId);
        return 0L;
    }

    @Override
    public List<ParticipationRequestDto> getRequestsByEvent(Long eventId) {
        log.warn("RequestService недоступен при получении запросов для события ID: {}, возвращаем пустой список", eventId);
        return new ArrayList<>();
    }

    @Override
    public ParticipationRequestDto createRequest(Long userId, CreateUpdateRequestDto requestDto) {
        log.warn("RequestService недоступен при создании запроса пользователем ID: {}", userId);
        return ParticipationRequestDto.builder()
                .id(-1L)
                .requester(userId)
                .event(requestDto.getEventId())
                .status("CANCELED")
                .created(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.warn("RequestService недоступен при отмене запроса ID: {} пользователем ID: {}", requestId, userId);
        return ParticipationRequestDto.builder()
                .id(requestId)
                .requester(userId)
                .status("CANCELED")
                .created(LocalDateTime.now().toString())
                .build();
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.warn("RequestService недоступен при получении запросов пользователя ID: {}, возвращаем пустой список", userId);
        return new ArrayList<>();
    }

    @Override
    public ParticipationRequestDto updateRequestStatus(Long requestId, String status) {
        log.warn("RequestService недоступен при обновлении статуса запроса ID: {}, статус: {}", requestId, status);
        return ParticipationRequestDto.builder()
                .id(requestId)
                .event(-1L)
                .requester(-1L)
                .status(status)
                .created(LocalDateTime.now().toString())
                .build();
    }
}