package com.fintech.account.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransferEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publish(String event) {
        kafkaTemplate.send("transfer.completed", event);
    }

}
