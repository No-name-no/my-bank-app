package org.mnuykin.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@Getter
@NoArgsConstructor
@Table(schema = "accounts", name = "transactions")
public class Tran {
    @Id
    @Column("id")
    private String transactionId;

    @Column("created_at")
    private OffsetDateTime createdAt;

    public Tran(String transactionId) {
        this.transactionId = transactionId;
        this.createdAt = OffsetDateTime.now();
    }
}
