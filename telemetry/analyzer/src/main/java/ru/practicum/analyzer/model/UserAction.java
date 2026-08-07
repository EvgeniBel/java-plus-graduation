package ru.practicum.analyzer.model;

import jakarta.persistence.*;
import lombok.*;
import ru.practicum.telemetry.messages.ActionType;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_actions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
    private ActionType actionType;

    @Column(name = "weight", nullable = false)
    private Integer weight;

    @Column(name = "action_time", nullable = false)
    private LocalDateTime actionTime;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}

