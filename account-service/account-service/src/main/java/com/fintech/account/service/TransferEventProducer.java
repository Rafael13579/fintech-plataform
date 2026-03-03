package com.fintech.account.service;

import com.fintech.account.dto.TransferCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferEventProducer {

    private final KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate;

    public void publish(TransferCompletedEvent event) {
        kafkaTemplate.send("transfer.completed", event.transactionId(), event);
    }

}
