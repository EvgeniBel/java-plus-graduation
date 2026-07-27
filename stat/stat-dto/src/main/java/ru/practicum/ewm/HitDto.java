package ru.practicum.ewm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.ewm.annotation.ValidIp;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HitDto {
    Long id;

    @NotBlank(message = "app не может быть пустым")
    @Size(max = 255, message = "app не может быть длиннее 255 символов")
    String app;

    @NotBlank(message = "uri не может быть пустым")
    @Size(max = 512, message = "uri не может быть длиннее 512 символов")
    String uri;

    @NotBlank(message = "ip не может быть пустым")
    @ValidIp
    String ip;

    @NotBlank(message = "timestamp не может быть пустым")
    @Pattern(
            regexp = "^\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}$",
            message = "Неверный формат даты. Используйте yyyy-MM-dd HH:mm:ss"
    )
    String timestamp;

    public HitDto(
            String app,
            String uri,
            String ip,
            String timestamp
    ) {
        this.app = app;
        this.uri = uri;
        this.ip = ip;
        this.timestamp = timestamp;
    }
}