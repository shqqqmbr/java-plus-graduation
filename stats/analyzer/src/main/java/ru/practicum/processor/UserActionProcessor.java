package ru.practicum.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaProperties;
import ru.practicum.service.UserActionService;
import ru.practicum.stats.avro.UserActionAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class UserActionProcessor implements Runnable {
    private final KafkaConsumer<Long, UserActionAvro> consumer;
    private final UserActionService userActionService;
    private final KafkaProperties kafkaConfig;

    public UserActionProcessor(KafkaProperties kafkaConfig, UserActionService userActionService) {
        this.kafkaConfig = kafkaConfig;
        consumer = new KafkaConsumer<>(kafkaConfig.getUserActionConsumerProps());
        this.userActionService = userActionService;
    }

    @Override
    public void run() {
        log.info("Запуск UserActionProcessor.");
        try {
            consumer.subscribe(List.of(kafkaConfig.getUserActionsTopic()));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            while (true) {
                ConsumerRecords<Long, UserActionAvro> records = consumer.poll(Duration.ofMillis(500));
                if (records.isEmpty()) continue;

                for (ConsumerRecord<Long, UserActionAvro> record : records) {
                    log.info("Запрос действий пользователя: topic = {}, partition = {}, offset = {}, value = {}",
                            record.topic(), record.partition(), record.offset(), record.value());

                    userActionService.addAction(record.value());

                    log.info("Действия пользователя успешно добавлены.");
                }

                consumer.commitAsync((offsets, exception) -> {
                    if (exception != null) {
                        log.warn("Ошибка фиксации оффсетов. Offset: {}", offsets, exception);
                    }
                });
            }

        } catch (WakeupException e) {
            log.info("UserActionProcessor получил WakeupException, завершение работы");
        } catch (Exception e) {
            log.error("Ошибка обработки user actions", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Закрываем consumer");
                consumer.close();
            }
        }
    }

    public void stop() {
        consumer.wakeup();
    }
}