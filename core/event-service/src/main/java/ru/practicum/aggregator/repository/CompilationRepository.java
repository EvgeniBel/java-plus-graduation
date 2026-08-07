package ru.practicum.aggregator.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.practicum.aggregator.model.Compilation;


@Repository
public interface CompilationRepository extends JpaRepository<Compilation, Long> {
    Page<Compilation> findAllByPinned(Boolean pinned, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Compilation c LEFT JOIN FETCH c.events WHERE c.id = :compId")
    Compilation findByIdWithEvents(Long compId);
}
