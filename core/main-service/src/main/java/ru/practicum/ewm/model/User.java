package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.model.event.Event;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "email", nullable = false, unique = true, length = 254)
    String email;

    @Column(name = "name", nullable = false, length = 250)
    String name;

    @OneToMany(mappedBy = "initiator", fetch = FetchType.LAZY)
    List<Event> events;

}