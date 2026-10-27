package org.mnuykin.config;

import io.micrometer.observation.ObservationRegistry;
import org.apache.kafka.clients.admin.NewTopic;
import org.mnuykin.dto.NotificationsRq;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.Map;

@Configuration
@Profile("!contract")
public class KafkaConfig {
    public static final String ACCOUNTS = "accounts-service";

    @Bean
    public ProducerFactory<String, NotificationsRq> notificationProducerFactory(
            KafkaProperties kafkaProperties) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, NotificationsRq> notificationKafkaTemplate(
            ProducerFactory<String, NotificationsRq> notificationProducerFactory,
            ObservationRegistry observationRegistry) {
        KafkaTemplate<String, NotificationsRq> template = new KafkaTemplate<>(notificationProducerFactory);
        template.setObservationEnabled(true);
        template.setObservationRegistry(observationRegistry);
        return template;
    }

    @Bean
    public NewTopic accountsService() {
        return TopicBuilder.name(ACCOUNTS).partitions(3).replicas(3).build();
    }
}
