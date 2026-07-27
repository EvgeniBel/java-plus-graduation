package ru.practicum.ewm.dto.compilation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateCompilationDto {

    List<Long> events;
    Boolean pinned;
    @NotBlank
    @Size(max = 50, message = "Длина названия подборки должна быть не более 50 символов")
    String title;

}
