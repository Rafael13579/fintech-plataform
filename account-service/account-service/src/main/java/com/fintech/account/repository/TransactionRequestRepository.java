package com.fintech.account.repository;

import com.fintech.account.model.TransactionRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransactionRequestRepository
        extends JpaRepository<TransactionRequest, Long> {

    Optional<TransactionRequest> findByIdempotencyKey(String key);
}