package ru.practicum.avro.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;

@Slf4j
@UtilityClass
public class AvroUtils {

    public static Schema getSchema(Class<? extends SpecificRecordBase> clazz) {
        try {
            return (Schema) clazz.getMethod("getClassSchema").invoke(null);
        } catch (Exception e) {
            log.error("Ошибка получения схемы для класса: {}", clazz.getName(), e);
            throw new RuntimeException("Не удалось получить схему Avro", e);
        }
    }

    public static String getSchemaString(Class<? extends SpecificRecordBase> clazz) {
        return getSchema(clazz).toString(true);
    }
}
