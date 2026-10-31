package org.mnuykin.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.r2dbc.postgresql.codec.Json;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mnuykin.dto.NotificationsRq;
import org.mnuykin.dto.OutboxStatus;
import org.mnuykin.entity.OutboxEvent;
import org.mnuykin.producer.NotificationProducer;
import org.mnuykin.repository.OutboxRepository;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherKafkaUnavailableTest {

    private static final String LOGIN = "login1";
    private static final long EVENT_ID = 100L;

    @Mock private OutboxRepository outboxRepository;
    @Mock private NotificationProducer notificationProducer;
    @Mock private ObjectMapper objectMapper;
    @Mock private OutboxEvent event;
    @Mock private Json payload;
    @Mock private NotificationsRq notificationsRq;

    private OutboxProperties properties;
    private SimpleMeterRegistry meterRegistry;
    private OutboxPublisher outboxPublisher;

    @BeforeEach
    void setUp() {
        properties = new OutboxProperties();
        properties.setBatchSize(10);

        meterRegistry = new SimpleMeterRegistry();
        outboxPublisher = new OutboxPublisher(
                outboxRepository,
                notificationProducer,
                objectMapper,
                properties,
                meterRegistry
        );
    }

    private void givenKafkaIsUnavailable(int retryCount) throws Exception {
        properties.setMaxAttempts(3);

        when(event.getId()).thenReturn(EVENT_ID);
        when(event.getLogin()).thenReturn(LOGIN);
        when(event.getRetryCount()).thenReturn(retryCount);
        when(event.getPayload()).thenReturn(payload);
        when(payload.asString()).thenReturn("{}");

        when(outboxRepository.findByStatusOrderById(eq(OutboxStatus.PENDING), any(PageRequest.class)))
                .thenReturn(Flux.just(event));

        when(objectMapper.readValue(anyString(), eq(NotificationsRq.class)))
                .thenReturn(notificationsRq);

        // Kafka недоступна — продюсер отдаёт ошибку в реактивной цепочке
        when(notificationProducer.send(notificationsRq))
                .thenReturn(Mono.error(new RuntimeException("Kafka broker is unavailable")));
    }

    @Test
    void whenKafkaUnavailableAndAttemptsRemain_thenEventIsScheduledForRetry() throws Exception {
        // given: retryCount=0, maxAttempts=3 → следующая попытка 1 < 3
        givenKafkaIsUnavailable(0);
        when(outboxRepository.incrementRetry(EVENT_ID)).thenReturn(Mono.empty());

        // when
        outboxPublisher.poll();

        // then
        verify(outboxRepository, timeout(1000)).incrementRetry(EVENT_ID);
        verify(outboxRepository, never()).markFailed(anyLong());
        verify(outboxRepository, never()).markCompleted(anyLong());
        assertThat(failureCounter()).isEqualTo(1.0);
    }

    @Test
    void whenKafkaUnavailableAndMaxAttemptsReached_thenEventIsMarkedFailed() throws Exception {
        // given: retryCount=2, maxAttempts=3 → следующая попытка 3 >= 3
        givenKafkaIsUnavailable(2);
        when(outboxRepository.markFailed(EVENT_ID)).thenReturn(Mono.empty());

        // when
        outboxPublisher.poll();

        // then
        verify(outboxRepository, timeout(1000)).markFailed(EVENT_ID);
        verify(outboxRepository, never()).incrementRetry(anyLong());
        verify(outboxRepository, never()).markCompleted(anyLong());
        assertThat(failureCounter()).isEqualTo(1.0);
    }

    private double failureCounter() {
        return meterRegistry.get("notification.failures")
                .tag("login", LOGIN)
                .counter()
                .count();
    }
}