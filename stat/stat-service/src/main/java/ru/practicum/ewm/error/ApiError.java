package ru.practicum.ewm.error;

import lombok.Value;
import org.springframework.http.HttpStatus;

@Value
public class ApiError {
    HttpStatus status;
    String description;
    String error;
    String stackTrace;
}