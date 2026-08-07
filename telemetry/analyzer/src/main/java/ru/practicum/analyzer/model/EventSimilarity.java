package ru.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "event_similarities", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"event_a_id", "event_b_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSimilarity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_a_id", nullable = false)
    private Long eventAId;

    @Column(name = "event_b_id", nullable = false)
    private Long eventBId;

    @Column(name = "similarity_score", nullable = false)
    private Double similarityScore;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;
}