package ru.practicum.dto.event;

import lombok.Value;

@Value
public class ConfirmedRequestCount {
    Long eventId;
    Long count;
}