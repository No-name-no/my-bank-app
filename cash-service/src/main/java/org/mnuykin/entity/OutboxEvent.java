package org.mnuykin.entity;

import io.r2dbc.postgresql.codec.Json;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.mnuykin.dto.OutboxStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(schema = "cash", name = "outbox")
public class OutboxEvent {
    @Id
    @Column("id")
    private Long id;
    @Column("login")
    private String login;

    private Json payload;
    private OutboxStatus status;
    @Column("retry_count")
    private int retryCount;
    @Column("update_at")
    private OffsetDateTime updateAt;
}
