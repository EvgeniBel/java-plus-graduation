package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.dto.request.RequestStatus;
import ru.practicum.model.ParticipationRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface RequestRepository extends JpaRepository<ParticipationRequest, Long> {

    @Query("SELECT r FROM ParticipationRequest r WHERE r.requesterId = :userId")
    List<ParticipationRequest> findAllByUserId(@Param("userId") Long userId);

    Long countByEventIdAndStatus(Long eventId, RequestStatus status);

    @Query("SELECT r.eventId, COUNT(r.id) " +
            "FROM ParticipationRequest r " +
            "WHERE r.eventId IN :eventIds AND r.status = :status " +
            "GROUP BY r.eventId")
    List<Object[]> countConfirmedRequestsByEventIds(
            @Param("eventIds") List<Long> eventIds,
            @Param("status") RequestStatus status
    );

    List<ParticipationRequest> findAllByEventId(Long eventId);

    Optional<ParticipationRequest> findByRequesterIdAndEventId(Long userId, Long eventId);
}