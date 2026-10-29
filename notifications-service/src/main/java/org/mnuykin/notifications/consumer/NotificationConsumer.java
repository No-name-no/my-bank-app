package org.mnuykin.notifications.consumer;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.mnuykin.notifications.dto.NotificationsRq;
import org.mnuykin.notifications.exception.InvalidNotificationException;
import org.mnuykin.notifications.service.NotificationService;
import org.springframework.core.log.LogAccessor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.listener.BatchListenerFailedException;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaUtils;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.SerializationUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {
    private final NotificationService notificationService;
    private final Validator validator;

    @KafkaListener(topics = {"accounts-service", "cash-service", "transfer-service"},
            containerFactory = "batchKafkaListenerContainerFactory"
    )
    public void listenNotification(List<ConsumerRecord<String, NotificationsRq>> records,
                                   Acknowledgment ack){
        if(records.isEmpty()){
            ack.acknowledge();
            return;
        }

        for (int i = 0; i < records.size(); i++) {
            ConsumerRecord<String, NotificationsRq> record = records.get(i);
            NotificationsRq rq = record.value();

            if (rq == null) {
                LogAccessor logAccessor = new LogAccessor(NotificationConsumer.class);
                DeserializationException deserEx = SerializationUtils.getExceptionFromHeader(
                        record,
                        KafkaUtils.VALUE_DESERIALIZER_EXCEPTION_HEADER,
                        logAccessor
                );

                if (deserEx != null) {
                    log.error("Deserialization failed for record at index {} (topic={}, partition={}, offset={})",
                            i, record.topic(), record.partition(), record.offset(), deserEx);
                    throw new BatchListenerFailedException("Deserialization failed", deserEx, i);
                }

                log.warn("Null payload at index {} (topic={}, partition={}, offset={})",
                        i, record.topic(), record.partition(), record.offset());
                throw new BatchListenerFailedException("Null payload", new InvalidNotificationException("Null payload"), i);
            }

            Set<ConstraintViolation<NotificationsRq>> violations = validator.validate(rq);
            if (!violations.isEmpty()) {
                log.error("Validation failed for record at index {} (offset={}): {}",
                        i, record.offset(), violations);
                throw new BatchListenerFailedException("Validation failed",
                        new InvalidNotificationException(violations.stream()
                                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                                .collect(Collectors.joining("; "))
                        ), i
                );
            }
            try {
                notificationService.send(rq);
            } catch (Exception e) {
                log.error("Failed to send notification for record at index {} (offset={})",
                        i, record.offset(), e);
                throw new BatchListenerFailedException("Send failed", e, i);
            }
        }

        ack.acknowledge();
        log.info("Batch of {} records acknowledged", records.size());
    }
}
