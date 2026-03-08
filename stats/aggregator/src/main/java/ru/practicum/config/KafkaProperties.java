package ru.practicum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Getter
@Setter
@Component
@ConfigurationProperties("aggregator.kafka")
public class KafkaProperties {
    private Properties producerProps;
    private Properties consumerProps;
    private String userActionsTopic;
    private String eventsSimilarityTopic;
}