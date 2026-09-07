package ru.practicum.aggregator.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.aggregator.model.Event;
import ru.practicum.aggregator.model.EventState;

import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, CustomEventRepository {

    @Query("SELECT e FROM Event e WHERE e.initiatorId = :initiatorId ORDER BY e.eventDate ASC")
    List<Event> findAllByInitiatorIdOrderByEventDateAsc(
            @Param("initiatorId") Long initiatorId,
            Pageable pageable
    );

    @Query("SELECT e FROM Event e WHERE e.id IN :eventIds ORDER BY e.id ASC")
    List<Event> findAllByIdInOrderByIdAsc(@Param("eventIds") List<Long> eventIds);


    boolean existsByIdAndState(Long id, EventState state);

    @Query("SELECT COUNT(e) > 0 FROM Event e WHERE e.categoryId = :categoryId")
    boolean existsByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT e FROM Event e WHERE e.state = :state ORDER BY e.eventDate ASC")
    List<Event> findAllByStateOrderByEventDateAsc(
            @Param("state") EventState state,
            Pageable pageable
    );
}