package org.mnuykin.notifications.config;

import brave.Tracing;
import brave.kafka.clients.KafkaTracing;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.mnuykin.notifications.dto.NotificationsRq;
import org.mnuykin.notifications.exception.InvalidNotificationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
public class KafkaConfig {
    /**
     * ConsumerFactory собирается из application.properties через KafkaProperties.
     * Добавлено оборачивание в KafkaTracing для корректной трассировки входящих сообщений.
     */
    @Bean
    public ConsumerFactory<String, NotificationsRq> notificationConsumerFactory(
            org.springframework.boot.kafka.autoconfigure.KafkaProperties kafkaProperties,
            Tracing tracing) { // Внедряем Brave Tracing
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        DefaultKafkaConsumerFactory<String, NotificationsRq> factory =
                new DefaultKafkaConsumerFactory<>(props);

        // Оборачиваем консьюмер в Brave tracing для извлечения контекста из заголовков Kafka
        factory.addPostProcessor(consumer ->
                KafkaTracing.create(tracing).consumer(consumer));

        return factory;
    }

    /**
     * Отдельная фабрика для пакетного слушателя с ручным коммитом и DLQ.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationsRq> batchKafkaListenerContainerFactory(
            ConsumerFactory<String, NotificationsRq> notificationConsumerFactory,
            KafkaTemplate<String, NotificationsRq> kafkaTemplate){

        ConcurrentKafkaListenerContainerFactory<String, NotificationsRq> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationConsumerFactory);

        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);

        // DLQ: сбойная запись (индекс которой указан в BatchListenerFailedException) уходит в топики <topic>-dlt
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + "-dlt", record.partition())
        );

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 2L));

        // Ошибки десериализации и валидации сразу в DLQ
        errorHandler.addNotRetryableExceptions(
                DeserializationException.class,
                InvalidNotificationException.class
        );

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }

    /**
     * DLQ-топики
     */
    @Bean
    public NewTopic accountsServiceDlt() {
        return TopicBuilder.name("accounts-service-dlt").partitions(3).replicas(3).build();
    }

    @Bean
    public NewTopic cashServiceDlt() {
        return TopicBuilder.name("cash-service-dlt").partitions(3).replicas(3).build();
    }

    @Bean
    public NewTopic transferServiceDlt() {
        return TopicBuilder.name("transfer-service-dlt").partitions(3).replicas(3).build();
    }
}