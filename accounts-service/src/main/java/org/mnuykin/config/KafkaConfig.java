package org.mnuykin.config;

import brave.Tracing;
import brave.kafka.clients.KafkaTracing;
import org.apache.kafka.clients.admin.NewTopic;
import org.mnuykin.dto.NotificationsRq;
import org.springframework.beans.factory.ObjectProvider;
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
            KafkaProperties kafkaProperties,
            ObjectProvider<Tracing> tracingProvider) {
        Map<String, Object> props = kafkaProperties.buildProducerProperties();
        DefaultKafkaProducerFactory<String, NotificationsRq> factory =
                new DefaultKafkaProducerFactory<>(props);

        tracingProvider.ifAvailable(tracing -> {
            KafkaTracing kafkaTracing = KafkaTracing.create(tracing);
            factory.addPostProcessor(kafkaTracing::producer);
        });

        return factory;
    }

    @Bean
    public KafkaTemplate<String, NotificationsRq> notificationKafkaTemplate(
            ProducerFactory<String, NotificationsRq> notificationProducerFactory) {
        return new KafkaTemplate<>(notificationProducerFactory);
    }

    @Bean
    public NewTopic accountsService() {
        return TopicBuilder.name(ACCOUNTS).partitions(3).replicas(3).build();
    }
}
