package ru.practicum.ewm.dto.compilation;

import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateCompilationDto {

    Long id;
    List<Long> events;
    Boolean pinned;

    @Size(max = 50, message = "Длина названия подборки должна быть не более 50 символов")
    String title;

}
