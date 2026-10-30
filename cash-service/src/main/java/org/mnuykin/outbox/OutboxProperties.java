package org.mnuykin.outbox;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "outbox")
@Getter
@Setter
public class OutboxProperties {
    private long pollIntervalMs = 1000;
    private int batchSize = 100;
    private int maxAttempts = 10;
}