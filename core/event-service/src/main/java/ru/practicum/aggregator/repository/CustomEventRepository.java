package ru.practicum.aggregator.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.dto.event.AdminEventRequestParam;
import ru.practicum.dto.event.PublicEventRequestParam;
import ru.practicum.aggregator.model.Event;

import java.util.List;


public interface CustomEventRepository {

    List<Event> findByAdminRequest(AdminEventRequestParam param);

    List<Event> findByAdminRequest(AdminEventRequestParam param, Pageable pageable);

    List<Event> findByAdminRequest(AdminEventRequestParam param, Pageable pageable, Sort sort);

    long countByAdminRequest(AdminEventRequestParam param);

    List<Event> findByPublicRequest(PublicEventRequestParam param, Pageable pageable);
}