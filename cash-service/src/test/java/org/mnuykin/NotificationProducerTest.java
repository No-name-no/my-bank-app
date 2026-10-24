package org.mnuykin;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mnuykin.config.KafkaConfig;
import org.mnuykin.dto.NotificationsRq;
import org.mnuykin.producer.NotificationProducer;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import reactor.test.StepVerifier;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationProducerTest {

    @Mock
    private KafkaTemplate<String, NotificationsRq> kafkaTemplate;

    @InjectMocks
    private NotificationProducer producer;

    @Test
    void send_shouldComplete_whenKafkaSendSucceeds() {
        // given
        NotificationsRq rq = new NotificationsRq("login", "subject", "message");

        RecordMetadata meta = mock(RecordMetadata.class);
        when(meta.partition()).thenReturn(1);
        when(meta.offset()).thenReturn(42L);

        @SuppressWarnings("unchecked")
        SendResult<String, NotificationsRq> sendResult = mock(SendResult.class);
        when(sendResult.getRecordMetadata()).thenReturn(meta);

        when(kafkaTemplate.send(KafkaConfig.CASH, "login", rq))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        StepVerifier.create(producer.send(rq))
                .verifyComplete();

        verify(kafkaTemplate).send(KafkaConfig.CASH, "login", rq);
    }

    @Test
    void send_shouldPropagateError_whenKafkaSendFails() {
        // given
        NotificationsRq rq = new NotificationsRq("login", "subject", "message");

        CompletableFuture<SendResult<String, NotificationsRq>> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("broker down"));

        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenReturn(failed);

        StepVerifier.create(producer.send(rq))
                .expectErrorMatches(t ->
                        t instanceof RuntimeException && "broker down".equals(t.getMessage()))
                .verify();
    }
}