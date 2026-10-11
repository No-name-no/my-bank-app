package org.mnuykin.service;

import org.mnuykin.dto.TransferRq;
import org.mnuykin.dto.TransferRs;
import reactor.core.publisher.Mono;

public interface TransferService {
    Mono<TransferRs> transfer(String senderLogin, TransferRq rq);
}