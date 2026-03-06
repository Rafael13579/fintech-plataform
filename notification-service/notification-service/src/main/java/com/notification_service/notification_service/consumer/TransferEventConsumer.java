package com.notification_service.notification_service.consumer;

import com.notification_service.notification_service.event.TransferCompletedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class TransferEventConsumer {

    @KafkaListener(topics = "transfer.completed", groupId = "notification-service-group")
    public void listen(TransferCompletedEvent event) {
        System.out.println("Transfer completed of id: " + event.transactionId());
    }
}
