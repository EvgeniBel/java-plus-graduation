package ru.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
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
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "event_id", nullable = false)
    Long eventId;

    @Column(name = "action_type", nullable = false)
    @Enumerated(EnumType.STRING)
    ActionTypeAvro actionType;

    @Column(name = "weight", nullable = false)
    Double weight;

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
