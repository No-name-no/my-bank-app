package org.mnuykin.notifications.consumer;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mnuykin.notifications.dto.NotificationsRq;
import org.mnuykin.notifications.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(properties = {
        "spring.kafka.consumer.group-id=test-notification-group",
        "spring.kafka.admin.fail-fast=false",
        "spring.config.import=optional:configserver:",
        "spring.cloud.config.enabled=false",
        "spring.cloud.config.fail-fast=false",
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
@EmbeddedKafka(
        partitions = 3,
        topics = {
                "accounts-service", "cash-service", "transfer-service",
                "accounts-service-dlt", "cash-service-dlt", "transfer-service-dlt"
        },
        bootstrapServersProperty = "spring.kafka.bootstrap-servers"
)
@ActiveProfiles("test")
class NotificationConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, NotificationsRq> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafka;

    @MockitoBean
    private NotificationService notificationService;

    @BeforeEach
    void resetMock() {
        reset(notificationService);
    }

    @Test
    void shouldProcessSingleValidMessage() throws Exception {
        NotificationsRq rq = new NotificationsRq("user-single", "subject", "message");
        String key = "single-" + UUID.randomUUID();

        kafkaTemplate.send("accounts-service", key, rq).get(10, TimeUnit.SECONDS);

        verify(notificationService, timeout(15_000)).send(rq);
        assertNoDltRecord("accounts-service-dlt", key);
    }

    @Test
    void shouldProcessBatchOfValidMessages() throws Exception {
        NotificationsRq rq1 = new NotificationsRq("user-batch-1", "subject-1", "message-1");
        NotificationsRq rq2 = new NotificationsRq("user-batch-2", "subject-2", "message-2");
        NotificationsRq rq3 = new NotificationsRq("user-batch-3", "subject-3", "message-3");

        kafkaTemplate.send("cash-service", UUID.randomUUID().toString(), rq1).get(10, TimeUnit.SECONDS);
        kafkaTemplate.send("cash-service", UUID.randomUUID().toString(), rq2).get(10, TimeUnit.SECONDS);
        kafkaTemplate.send("cash-service", UUID.randomUUID().toString(), rq3).get(10, TimeUnit.SECONDS);

        verify(notificationService, timeout(15_000)).send(rq1);
        verify(notificationService, timeout(15_000)).send(rq2);
        verify(notificationService, timeout(15_000)).send(rq3);
    }

    @Test
    void shouldProcessMessagesFromAllSubscribedTopics() throws Exception {
        NotificationsRq rq1 = new NotificationsRq("u-a", "s-a", "m-a");
        NotificationsRq rq2 = new NotificationsRq("u-c", "s-c", "m-c");
        NotificationsRq rq3 = new NotificationsRq("u-t", "s-t", "m-t");

        kafkaTemplate.send("accounts-service", UUID.randomUUID().toString(), rq1).get(10, TimeUnit.SECONDS);
        kafkaTemplate.send("cash-service", UUID.randomUUID().toString(), rq2).get(10, TimeUnit.SECONDS);
        kafkaTemplate.send("transfer-service", UUID.randomUUID().toString(), rq3).get(10, TimeUnit.SECONDS);

        verify(notificationService, timeout(15_000)).send(rq1);
        verify(notificationService, timeout(15_000)).send(rq2);
        verify(notificationService, timeout(15_000)).send(rq3);
    }

    @Test
    void shouldRouteToDltWhenValidationFails() throws Exception {
        // пустой login → @NotBlank violation
        NotificationsRq invalid = new NotificationsRq("", "subj", "msg");
        String key = "invalid-" + UUID.randomUUID();

        kafkaTemplate.send("accounts-service", key, invalid).get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> dltRecord = awaitDltRecord("accounts-service-dlt", key);
        assertThat(dltRecord).isNotNull();
        assertThat(dltRecord.value())
                .contains("\"subject\":\"subj\"")
                .contains("\"message\":\"msg\"");

        verify(notificationService, after(500).never()).send(any());
    }

    @Test
    void shouldRouteToDltWhenDeserializationFails() throws Exception {
        String key = "bad-json-" + UUID.randomUUID();
        sendRawBytes("accounts-service", key, "this is definitely not json {{{".getBytes());

        ConsumerRecord<String, String> dltRecord = awaitDltRecord("accounts-service-dlt", key);
        assertThat(dltRecord).isNotNull();

        verify(notificationService, after(500).never()).send(any());
    }

    @Test
    void shouldRouteToDltWhenNotificationServiceThrows() throws Exception {
        NotificationsRq rq = new NotificationsRq("user-fail", "subj", "msg");
        String key = "fail-" + UUID.randomUUID();

        doThrow(new IllegalStateException("boom"))
                .when(notificationService).send(any());

        kafkaTemplate.send("transfer-service", key, rq).get(10, TimeUnit.SECONDS);

        ConsumerRecord<String, String> dltRecord = awaitDltRecord("transfer-service-dlt", key);
        assertThat(dltRecord).isNotNull();
        assertThat(dltRecord.value()).contains("user-fail");

        verify(notificationService, times(1)).send(rq);
    }

    private void sendRawBytes(String topic, String key, byte[] value) throws Exception {
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafka);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class);

        try (KafkaProducer<String, byte[]> producer = new KafkaProducer<>(producerProps)) {
            producer.send(new ProducerRecord<>(topic, key, value)).get(10, TimeUnit.SECONDS);
        }
    }

    private ConsumerRecord<String, String> awaitDltRecord(String topic, String expectedKey) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                embeddedKafka, "dlt-reader-" + UUID.randomUUID(), false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer());

        try (Consumer<String, String> consumer = cf.createConsumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, topic);
            long deadline = System.currentTimeMillis() + 15_000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records =
                        KafkaTestUtils.getRecords(consumer, Duration.ofMillis(500));
                for (ConsumerRecord<String, String> r : records) {
                    if (expectedKey.equals(r.key())) {
                        return r;
                    }
                }
            }
        }
        throw new AssertionError(
                "Ожидалась запись с key='" + expectedKey + "' в топике " + topic);
    }

    private void assertNoDltRecord(String topic, String key) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                embeddedKafka, "dlt-reader-" + UUID.randomUUID(), false);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer());

        try (Consumer<String, String> consumer = cf.createConsumer()) {
            embeddedKafka.consumeFromAnEmbeddedTopic(consumer, topic);
            long deadline = System.currentTimeMillis() + 1_000;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> records =
                        KafkaTestUtils.getRecords(consumer, Duration.ofMillis(200));
                for (ConsumerRecord<String, String> r : records) {
                    assertThat(r.key())
                            .as("Неожиданная DLT-запись с key=%s в %s", key, topic)
                            .isNotEqualTo(key);
                }
            }
        }
    }
}