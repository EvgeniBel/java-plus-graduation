package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.analyzer.model.EventSimilarity;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, Long> {

    @Query("SELECT e FROM EventSimilarity e WHERE e.eventAId = :eventId OR e.eventBId = :eventId")
    List<EventSimilarity> findByEventId(@Param("eventId") Long eventId);

    @Query("SELECT e FROM EventSimilarity e WHERE (e.eventAId = :eventA AND e.eventBId = :eventB) " +
            "OR (e.eventAId = :eventB AND e.eventBId = :eventA)")
    Optional<EventSimilarity> findByEventPair(@Param("eventA") Long eventA, @Param("eventB") Long eventB);

    @Query("SELECT e FROM EventSimilarity e WHERE e.eventAId IN :eventIds OR e.eventBId IN :eventIds")
    List<EventSimilarity> findByEventIds(@Param("eventIds") List<Long> eventIds);

    @Query("SELECT e FROM EventSimilarity e WHERE (e.eventAId = :eventId OR e.eventBId = :eventId) " +
            "ORDER BY e.similarityScore DESC")
    List<EventSimilarity> findTopSimilarByEventId(@Param("eventId") Long eventId);
}