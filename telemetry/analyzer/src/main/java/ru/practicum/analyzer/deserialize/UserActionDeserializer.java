package ru.practicum.analyzer.deserialize;

import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Component
public class UserActionDeserializer extends BaseAvroDeserializer<UserActionAvro> {

    public UserActionDeserializer() {
        super(UserActionAvro.getClassSchema());
    }
}
