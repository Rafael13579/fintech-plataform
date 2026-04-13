package com.fintech.account.outbox.publisher;

import com.fintech.account.outbox.model.OutboxEvent;
import com.fintech.account.outbox.repository.OutboxRepository;
import com.fintech.account.service.TransferEventProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxRepository repository;
    private final TransferEventProducer producer;

    @Scheduled(fixedDelay = 2000)
    public void publishEvents() {

        List<OutboxEvent> events = repository.findTop10ByPublishedFalseOrderByCreatedAt();

        for (OutboxEvent event : events) {
            try {
                producer.publish(event.getPayload());
                event.setPublished(true);
                repository.save(event);

            } catch (Exception e) {
                System.out.println("Failed to publish event: " + event.getId());
                e.printStackTrace();
            }
        }
    }
}