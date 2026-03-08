package ru.practicum.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Getter
@Setter
@Component
@ConfigurationProperties("analyzer.kafka")
public class KafkaProperties {
    private Properties userActionConsumerProps;
    private Properties eventSimilarityConsumerProps;
    private String userActionsTopic;
    private String eventsSimilarityTopic;
}