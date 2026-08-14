package ru.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_actions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "event_id"}),
        indexes = {
                @Index(name = "idx_user_actions_user_id", columnList = "user_id"),
                @Index(name = "idx_user_actions_event_id", columnList = "event_id"),
                @Index(name = "idx_user_actions_user_event", columnList = "user_id, event_id")
        })
public class UserAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(name = "action_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ActionTypeAvro actionType;

    @Column(name = "weight", nullable = false)
    private Integer weight;

    @Column(name = "timestamp", nullable = false)
    private Long timestamp;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
