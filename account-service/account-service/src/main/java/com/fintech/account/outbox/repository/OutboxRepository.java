package com.fintech.account.outbox.repository;

import com.fintech.account.outbox.model.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop10ByPublishedFalseOrderByCreatedAt();
}
