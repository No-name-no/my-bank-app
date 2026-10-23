package org.mnuykin.notifications.service;

import lombok.extern.slf4j.Slf4j;
import org.mnuykin.notifications.dto.NotificationsRq;
import reactor.core.publisher.Mono;

@Slf4j
public class NotificationService {
    public Mono<Void> send (NotificationsRq rq){
        log.info("Notification: recipient={}, subject={}, message={}",
                rq.login(), rq.subject(), rq.message()
        );
        return Mono.empty();
    }
}
