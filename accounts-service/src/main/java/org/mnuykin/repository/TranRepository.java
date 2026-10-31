package org.mnuykin.repository;

import org.mnuykin.entity.Tran;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface TranRepository extends ReactiveCrudRepository<Tran, String> {
}
