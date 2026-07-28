package ru.practicum.ewm.model.event;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.model.Category;
import ru.practicum.ewm.model.Compilation;
import ru.practicum.ewm.model.User;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "events")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "annotation", nullable = false, length = 2000)
    String annotation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @Column(name = "created_on", nullable = false)
    LocalDateTime createdOn;

    @Column(name = "description", nullable = false, length = 7000)
    String description;

    @Column(name = "event_date", nullable = false)
    LocalDateTime eventDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id", nullable = false)
    User initiator;

    @OneToOne
    @JoinColumn(name = "location_id", nullable = false)
    Location location;

    @Column(name = "paid", nullable = false)
    Boolean paid;

    @Column(name = "participant_limit", nullable = false)
    Integer participantLimit;

    @Column(name = "published_on")
    LocalDateTime publishedOn;

    @Column(name = "request_moderation", nullable = false)
    Boolean requestModeration;

    @Column(name = "state")
    @Enumerated(EnumType.STRING)
    EventState state;

    @Column(name = "title", nullable = false, length = 120)
    String title;

    @ManyToMany(mappedBy = "events", fetch = FetchType.LAZY)
    List<Compilation> compilations;
}