package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.analyzer.model.UserAction;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    Optional<UserAction> findByUserIdAndEventId(Long userId, Long eventId);

    @Query("SELECT ua.eventId FROM UserAction ua WHERE ua.userId = :userId")
    List<Long> findEventIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT SUM(ua.weight) FROM UserAction ua WHERE ua.eventId IN :eventIds")
    Long sumWeightsByEventIds(@Param("eventIds") List<Long> eventIds);

    @Modifying
    @Transactional
    @Query("UPDATE UserAction ua SET ua.actionType = :actionType, ua.weight = :weight, " +
            "ua.timestamp = :timestamp, ua.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE ua.userId = :userId AND ua.eventId = :eventId")
    int updateUserAction(@Param("userId") Long userId,
                         @Param("eventId") Long eventId,
                         @Param("actionType") ru.practicum.ewm.stats.avro.ActionTypeAvro actionType,
                         @Param("weight") Integer weight,
                         @Param("timestamp") Long timestamp);
}
