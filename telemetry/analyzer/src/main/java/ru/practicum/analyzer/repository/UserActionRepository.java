package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.analyzer.model.UserMaxWeight;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserActionRepository extends JpaRepository<UserMaxWeight, Long> {

    Optional<UserMaxWeight> findByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT u FROM UserMaxWeight u WHERE u.userId = :userId ORDER BY u.updatedAt DESC")
    List<UserMaxWeight> findRecentByUserId(@Param("userId") Long userId);

    @Query("SELECT u.eventId FROM UserMaxWeight u WHERE u.userId = :userId")
    List<Long> findEventIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT u.eventId, u.maxWeight FROM UserMaxWeight u WHERE u.userId = :userId AND u.eventId IN :eventIds")
    List<Object[]> findUserWeightsForEvents(
            @Param("userId") Long userId,
            @Param("eventIds") List<Long> eventIds
    );

    @Query("SELECT SUM(u.maxWeight) FROM UserMaxWeight u WHERE u.eventId = :eventId")
    Double sumWeightsByEvent(@Param("eventId") Long eventId);

    @Query("SELECT u.eventId, SUM(u.maxWeight) FROM UserMaxWeight u WHERE u.eventId IN :eventIds GROUP BY u.eventId")
    List<Object[]> sumWeightsByEvents(@Param("eventIds") List<Long> eventIds);
}