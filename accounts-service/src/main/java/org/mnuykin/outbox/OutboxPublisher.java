package org.mnuykin.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mnuykin.dto.NotificationsRq;
import org.mnuykin.dto.OutboxStatus;
import org.mnuykin.entity.OutboxEvent;
import org.mnuykin.producer.NotificationProducer;
import org.mnuykin.repository.OutboxRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxRepository outboxRepository;
    private final NotificationProducer notificationProducer;
    private final ObjectMapper objectMapper;
    private final OutboxProperties properties;
    private final MeterRegistry meterRegistry;

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:1000}")
    public void poll() {
        outboxRepository.findByStatusOrderById(
                        OutboxStatus.PENDING,
                        PageRequest.of(0, properties.getBatchSize()))
                .concatMap(this::publish)
                .doOnError(ex -> log.error("Outbox polling failed", ex))
                .onErrorResume(ex -> Mono.empty())
                .subscribe();
    }

    private Mono<Void> publish(OutboxEvent event) {
        return deserialize(event)
                .flatMap(notificationProducer::send)
                .then(Mono.defer(() -> outboxRepository.markCompleted(event.getId())))
                .onErrorResume(ex -> {
                    Counter.builder("notification.failures")
                            .description("Невозможность отправки уведомления")
                            .tag("login", event.getLogin())
                            .register(meterRegistry)
                            .increment();

                    int nextAttempt = event.getRetryCount() + 1;
                    if (nextAttempt >= properties.getMaxAttempts()) {
                        log.error("Outbox id={} exceeded {} attempts, marking FAILED",
                                event.getId(), properties.getMaxAttempts(), ex);
                        return outboxRepository.markFailed(event.getId());
                    }
                    log.warn("Outbox id={} attempt {} failed, will retry",
                            event.getId(), nextAttempt, ex);
                    return outboxRepository.incrementRetry(event.getId());
                });
    }

    private Mono<NotificationsRq> deserialize(OutboxEvent event) {
        return Mono.fromCallable(() ->
                objectMapper.readValue(event.getPayload().asString(), NotificationsRq.class));
    }
}