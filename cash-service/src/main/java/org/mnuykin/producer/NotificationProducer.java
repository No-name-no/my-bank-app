package org.mnuykin.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mnuykin.config.KafkaConfig;
import org.mnuykin.dto.NotificationsRq;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationProducer {
    private final KafkaTemplate<String, NotificationsRq> kafkaTemplate;

    public Mono<Void> send(NotificationsRq rq) {
        return Mono.fromFuture(kafkaTemplate.send(KafkaConfig.CASH, rq.getLogin(), rq))
                .doOnSuccess(result -> {
                    assert result != null;
                    var meta = result.getRecordMetadata();
                    log.info("Notification sent: topic={} partition={} offset={} login={}",
                            KafkaConfig.CASH, meta.partition(), meta.offset(), rq.getLogin());
                })
                .doOnError(ex -> log.error(
                        "Failed to send notification to topic={} for login={}",
                        KafkaConfig.CASH, rq.getLogin(), ex))
                .then();
    }
}