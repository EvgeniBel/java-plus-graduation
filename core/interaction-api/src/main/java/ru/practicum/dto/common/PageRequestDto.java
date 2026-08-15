package ru.practicum.dto.common;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PageRequestDto {

    @PositiveOrZero
    @Builder.Default
    Integer from = 0;

    @Positive
    @Builder.Default
    Integer size = 10;

    public Integer getOffset() {
        return from;
    }

    public Integer getLimit() {
        return size;
    }
}