package com.fintech.account.service;

import com.fintech.account.event.TransferCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferEventProducer {

    private final KafkaTemplate<String, TransferCompletedEvent> kafkaTemplate;

    public void publish(String event) {
        kafkaTemplate.send("transfer.completed", event.transactionId(), event);
    }

}
