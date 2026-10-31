package org.mnuykin.repository;

import org.mnuykin.dto.OutboxStatus;
import org.mnuykin.entity.OutboxEvent;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.data.domain.Pageable;

public interface OutboxRepository extends ReactiveCrudRepository<OutboxEvent, Long> {

    Flux<OutboxEvent> findByStatusOrderById(OutboxStatus status, Pageable pageable);
    @Modifying
    @Query("UPDATE outbox SET status = 'COMPLETED', update_at = now() WHERE id = :id")
    Mono<Void> markCompleted(Long id);

    @Modifying
    @Query("UPDATE outbox SET retry_count = retry_count + 1, update_at = now() WHERE id = :id")
    Mono<Void> incrementRetry(Long id);

    @Modifying
    @Query("UPDATE outbox SET status = 'FAILED', update_at = now() WHERE id = :id")
    Mono<Void> markFailed(Long id);
}