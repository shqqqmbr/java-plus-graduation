package ru.practicum.service;


import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.practicum.config.KafkaConfig;
import ru.practicum.stats.avro.ActionTypeAvro;
import ru.practicum.stats.avro.UserActionAvro;
import ru.practicum.stats.proto.ActionTypeProto;
import ru.practicum.stats.proto.UserActionProto;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserActionServiceImpl implements UserActionService {

    private final Producer<Long, SpecificRecordBase> producer;
    private final KafkaConfig kafkaConfig;

    @Override
    public void collectUserAction(UserActionProto userActionProto) {

        log.info("UserActionService: обработка UserActionProto, eventId={}", userActionProto.getEventId());

        UserActionAvro userActionAvro = new UserActionAvro();
        userActionAvro.setUserId(userActionProto.getUserId());
        userActionAvro.setEventId(userActionProto.getEventId());
        userActionAvro.setActionType(toActionTypeAvro(userActionProto.getActionType()));
        userActionAvro.setTimestamp(Instant.ofEpochSecond(userActionProto.getTimestamp().getSeconds(),
                userActionProto.getTimestamp().getNanos()));


        send(kafkaConfig.getKafkaProperties().getUserActionTopic(),
                userActionAvro.getEventId(),
                userActionAvro.getTimestamp().toEpochMilli(),
                userActionAvro);

    }

    public static ActionTypeAvro toActionTypeAvro(ActionTypeProto actionTypeProto) {
        return switch (actionTypeProto) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> null;
        };
    }

    private void send(String topic, Long key, Long timestamp, SpecificRecordBase specificRecordBase) {
        ProducerRecord<Long, SpecificRecordBase> rec = new ProducerRecord<>(
                topic,
                null,
                timestamp,
                key,
                specificRecordBase);
        producer.send(rec, (metadata, exception) -> {
            if (exception != null) {
                log.error("Kafka: сообщение не отправлено, topic: {}", topic, exception);
            } else {
                log.info("Kafka: сообщение отправлено, topic: {}, partition: {}, offset: {}",
                        metadata.topic(), metadata.partition(), metadata.offset());
            }
        });
    }

    @PreDestroy
    private void close() {
        if (producer != null) {
            producer.flush();
            producer.close();
        }
    }
}