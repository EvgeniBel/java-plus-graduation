package ru.practicum.analyzer.repository;

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

    @Query("SELECT COALESCE(SUM(ua.weight), 0) FROM UserAction ua WHERE ua.eventId = :eventId")
    Long sumWeightByEventId(@Param("eventId") Long eventId);
}
