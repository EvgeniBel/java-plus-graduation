package ru.practicum.analyzer.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.analyzer.model.UserAction;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT ua.eventId FROM UserAction ua WHERE ua.userId = :userId")
    List<Long> findEventIdsByUserId(@Param("userId") Long userId);

    // Получение последних действий пользователя
    @Query("SELECT ua FROM UserAction ua WHERE ua.userId = :userId ORDER BY ua.timestamp DESC")
    List<UserAction> findLastUserActions(@Param("userId") Long userId, Pageable pageable);

    // Сумма максимальных весов для каждого пользователя (исправлено)
    @Query(value = "SELECT COALESCE(SUM(max_weight), 0) FROM (SELECT MAX(weight) as max_weight FROM user_actions WHERE event_id = :eventId GROUP BY user_id) sub", nativeQuery = true)
    Long sumMaxWeightsByEventId(@Param("eventId") Long eventId);

    // Получение всех действий по мероприятию с их весами
    @Query("SELECT ua FROM UserAction ua WHERE ua.eventId = :eventId")
    List<UserAction> findByEventId(@Param("eventId") Long eventId);
}
