package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.model.event.Event;

import java.util.List;

@Entity
@Builder
@Getter
@Setter
@Table(name = "compilation")
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Compilation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "event_compilation",
            joinColumns = @JoinColumn(name = "compilation_id"),
            inverseJoinColumns = @JoinColumn(name = "event_id")
    )
    List<Event> events;

    @Column(name = "pinned")
    Boolean pinned;

    @Column(name = "title")
    String title;

}
