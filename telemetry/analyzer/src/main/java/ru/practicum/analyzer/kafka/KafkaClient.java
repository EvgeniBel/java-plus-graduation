package ru.practicum.analyzer.kafka;


import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import ru.practicum.analyzer.deserialize.BaseAvroDeserializer;

public interface KafkaClient<T extends SpecificRecordBase> {

    Consumer<String, T> getConsumer(String groupId, Class<? extends BaseAvroDeserializer<T>> deserializer);

}