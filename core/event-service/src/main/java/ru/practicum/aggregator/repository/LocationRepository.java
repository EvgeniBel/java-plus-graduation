package ru.practicum.aggregator.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.aggregator.model.Location;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
}