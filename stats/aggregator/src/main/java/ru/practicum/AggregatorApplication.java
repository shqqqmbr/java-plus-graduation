package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.starter.AggregationStarter;

@EnableDiscoveryClient
@SpringBootApplication
@ConfigurationPropertiesScan
public class AggregatorApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AggregatorApplication.class, args);
        AggregationStarter aggregationStarter = context.getBean(AggregationStarter.class);

        Runtime.getRuntime().addShutdownHook(new Thread(aggregationStarter::stop));

        Thread aggregatorThread = new Thread(aggregationStarter::start);
        aggregatorThread.setName("AggregatorThread");
        aggregatorThread.start();
    }
}