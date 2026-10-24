package org.mnuykin.notifications.service;

import lombok.extern.slf4j.Slf4j;
import org.mnuykin.notifications.dto.NotificationsRq;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {
    public void send (NotificationsRq rq){
        log.info("Notification: recipient={}, subject={}, message={}",
                rq.login(), rq.subject(), rq.message()
        );
    }
}