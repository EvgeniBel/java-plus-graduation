package ru.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "event_similarities",
        uniqueConstraints = @UniqueConstraint(columnNames = {"event_a", "event_b"}),
        indexes = {
                @Index(name = "idx_event_similarities_event_a", columnList = "event_a"),
                @Index(name = "idx_event_similarities_event_b", columnList = "event_b"),
                @Index(name = "idx_event_similarities_score", columnList = "score DESC")
        })
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventSimilarity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "event_a", nullable = false)
    Long eventA;

    @Column(name = "event_b", nullable = false)
    Long eventB;

    @Column(name = "score", nullable = false)
    Double score;

    @Column(name = "timestamp", nullable = false)
    Long timestamp;

    @Column(name = "updated_at")
    Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
