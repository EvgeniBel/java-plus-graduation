package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.EventSimilarity;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    Optional<EventSimilarity> findByEventAAndEventB(Long eventA, Long eventB);

    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA = :eventId OR es.eventB = :eventId " +
            "ORDER BY es.score DESC")
    List<EventSimilarity> findSimilarEventsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT es FROM EventSimilarity es WHERE es.eventA = :eventId OR es.eventB = :eventId")
    List<EventSimilarity> findAllByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Transactional
    @Query("UPDATE EventSimilarity es SET es.score = :score, es.timestamp = :timestamp, " +
            "es.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE es.eventA = :eventA AND es.eventB = :eventB")
    int updateSimilarity(@Param("eventA") Long eventA,
                         @Param("eventB") Long eventB,
                         @Param("score") Double score,
                         @Param("timestamp") Long timestamp);
}
