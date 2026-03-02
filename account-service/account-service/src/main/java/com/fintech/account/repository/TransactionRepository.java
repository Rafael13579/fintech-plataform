package com.fintech.account.repository;

import com.fintech.account.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Optional<List<Transaction>> findByAccountIdOrderByCreatedAt(UUID accountId);

}
