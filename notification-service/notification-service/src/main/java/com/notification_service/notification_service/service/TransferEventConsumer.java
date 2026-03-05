package com.notification_service.notification_service.service;

import com.notification_service.notification_service.dto.TransferCompletedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransferEventConsumer {

    @KafkaListener(topics = "transfer.completed")
    public void listen(TransferCompletedEvent event) {
        System.out.println("Transfer completed of id: " + event.transactionId());
    }
}
