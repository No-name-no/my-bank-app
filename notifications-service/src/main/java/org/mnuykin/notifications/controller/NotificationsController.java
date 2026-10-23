package org.mnuykin.notifications.controller;

import lombok.extern.slf4j.Slf4j;
import org.mnuykin.notifications.controller.dto.NotificationsRq;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequestMapping("/notifications")
public class NotificationsController {

    @PostMapping
    public Mono<Void> notify (@RequestBody NotificationsRq rq){
        log.info("Notification: recipient={}, subject={}, message={}",
                rq.login(), rq.subject(), rq.message()
        );
        return Mono.empty();
    }
}