package ru.practicum.aggregator.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import ru.practicum.constants.Constants;
import ru.practicum.dto.event.AdminEventRequestParam;
import ru.practicum.dto.event.PublicEventRequestParam;
import ru.practicum.aggregator.model.Event;
import ru.practicum.aggregator.model.QEvent;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomEventRepositoryImpl implements CustomEventRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Event> findByAdminRequest(AdminEventRequestParam param) {
        return findByAdminRequest(param, Pageable.unpaged());
    }

    @Override
    public List<Event> findByAdminRequest(AdminEventRequestParam param, Pageable pageable) {
        return findByAdminRequest(param, pageable, Sort.unsorted());
    }

    @Override
    public List<Event> findByAdminRequest(AdminEventRequestParam param, Pageable pageable, Sort sort) {
        QEvent event = QEvent.event;
        BooleanBuilder builder = buildAdminPredicates(param);

        var query = queryFactory
                .selectFrom(event)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        // Применяем сортировку, если она задана
        if (sort != null && sort.isSorted()) {
            sort.get().forEach(order -> {
                String property = order.getProperty();
                boolean ascending = order.isAscending();

                switch (property) {
                    case "id" -> query.orderBy(ascending ? event.id.asc() : event.id.desc());
                    case "title" -> query.orderBy(ascending ? event.title.asc() : event.title.desc());
                    case "annotation" -> query.orderBy(ascending ? event.annotation.asc() : event.annotation.desc());
                    case "eventDate" -> query.orderBy(ascending ? event.eventDate.asc() : event.eventDate.desc());
                    case "createdOn" -> query.orderBy(ascending ? event.createdOn.asc() : event.createdOn.desc());
                    case "publishedOn" -> query.orderBy(ascending ? event.publishedOn.asc() : event.publishedOn.desc());
                    case "state" -> query.orderBy(ascending ? event.state.asc() : event.state.desc());
                    default -> query.orderBy(event.id.asc());
                }
            });
        } else {
            query.orderBy(event.eventDate.asc());
        }

        return query.fetch();
    }

    @Override
    public long countByAdminRequest(AdminEventRequestParam param) {
        QEvent event = QEvent.event;
        BooleanBuilder builder = buildAdminPredicates(param);

        return queryFactory
                .select(event.count())
                .from(event)
                .where(builder)
                .fetchOne();
    }

    @Override
    public List<Event> findByPublicRequest(PublicEventRequestParam param, Pageable pageable) {
        QEvent event = QEvent.event;
        BooleanBuilder builder = buildPublicPredicates(param);

        var query = queryFactory
                .selectFrom(event)
                .where(builder)
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize());

        if (param.getSort() != null) {
            switch (param.getSort()) {
                case "VIEWS" -> {
                    query.orderBy(event.eventDate.asc());
                }
                case "EVENT_DATE" -> query.orderBy(event.eventDate.asc());
                default -> query.orderBy(event.eventDate.asc());
            }
        } else {
            query.orderBy(event.eventDate.asc());
        }

        return query.fetch();
    }

    private BooleanBuilder buildAdminPredicates(AdminEventRequestParam param) {
        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();

        if (param.getUsers() != null && !param.getUsers().isEmpty()) {
            builder.and(event.initiatorId.in(param.getUsers()));
        }

        if (param.getStates() != null && !param.getStates().isEmpty()) {
            builder.and(event.state.stringValue().in(param.getStates()));
        }

        if (param.getCategories() != null && !param.getCategories().isEmpty()) {
            builder.and(event.categoryId.in(param.getCategories()));
        }

        if (param.getRangeStart() != null && !param.getRangeStart().isBlank()) {
            LocalDateTime start = LocalDateTime.parse(param.getRangeStart(), Constants.FORMATTER);
            builder.and(event.eventDate.goe(start));
        }

        if (param.getRangeEnd() != null && !param.getRangeEnd().isBlank()) {
            LocalDateTime end = LocalDateTime.parse(param.getRangeEnd(), Constants.FORMATTER);
            builder.and(event.eventDate.loe(end));
        }

        return builder;
    }

    private BooleanBuilder buildPublicPredicates(PublicEventRequestParam param) {
        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(event.state.stringValue().eq("PUBLISHED"));

        if (param.getText() != null && !param.getText().isBlank()) {
            builder.and(event.annotation.containsIgnoreCase(param.getText())
                    .or(event.description.containsIgnoreCase(param.getText())));
        }

        if (param.getCategories() != null && !param.getCategories().isEmpty()) {
            builder.and(event.categoryId.in(param.getCategories()));
        }

        if (param.getPaid() != null) {
            builder.and(event.paid.eq(param.getPaid()));
        }

        if (param.getRangeStart() != null && !param.getRangeStart().isBlank()) {
            LocalDateTime start = LocalDateTime.parse(param.getRangeStart(), Constants.FORMATTER);
            builder.and(event.eventDate.goe(start));
        }

        if (param.getRangeEnd() != null && !param.getRangeEnd().isBlank()) {
            LocalDateTime end = LocalDateTime.parse(param.getRangeEnd(), Constants.FORMATTER);
            builder.and(event.eventDate.loe(end));
        }

        if ((param.getRangeStart() == null || param.getRangeStart().isBlank())
                && (param.getRangeEnd() == null || param.getRangeEnd().isBlank())) {
            builder.and(event.eventDate.goe(LocalDateTime.now()));
        }

        return builder;
    }
}