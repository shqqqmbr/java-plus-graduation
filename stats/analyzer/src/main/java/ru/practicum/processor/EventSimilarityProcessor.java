package ru.practicum.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaProperties;
import ru.practicum.service.EventSimilarityService;
import ru.practicum.stats.avro.EventSimilarityAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class EventSimilarityProcessor implements Runnable {
    private final KafkaConsumer<Long, EventSimilarityAvro> consumer;
    private final EventSimilarityService eventSimilarityService;
    private final KafkaProperties kafkaConfig;

    public EventSimilarityProcessor(KafkaProperties kafkaConfig, EventSimilarityService eventSimilarityService) {
        this.kafkaConfig = kafkaConfig;
        consumer = new KafkaConsumer<>(kafkaConfig.getEventSimilarityConsumerProps());
        this.eventSimilarityService = eventSimilarityService;
    }

    @Override
    public void run() {
        log.info("Запуск EventSimilarityProcessor.");
        try {
            consumer.subscribe(List.of(kafkaConfig.getEventsSimilarityTopic()));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

            while (true) {
                ConsumerRecords<Long, EventSimilarityAvro> records = consumer.poll(Duration.ofMillis(500));
                if (records.isEmpty()) continue;

                for (ConsumerRecord<Long, EventSimilarityAvro> record : records) {
                    log.info("Запрос схожести: topic = {}, partition = {}, offset = {}, value = {}",
                            record.topic(), record.partition(), record.offset(), record.value());

                    eventSimilarityService.addSimilarity(record.value());

                    log.info("Схожесть обработана.");
                }

                consumer.commitAsync((offsets, exception) -> {
                    if (exception != null) {
                        log.warn("Ошибка фиксации оффсетов. Offset: {}", offsets, exception);
                    }
                });
            }

        } catch (WakeupException ignored) {
            log.info("EventSimilarityProcessor получил WakeupException, завершение работы");
        } catch (Exception e) {
            log.error("Ошибка обработки event similarity", e);
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