package com.notification_service.notification_service.consumer;

import com.notification_service.notification_service.event.TransferCompletedEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class TransferEventConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "transfer.completed", groupId = "notification-service-group")
    public void listen(String message) {
        try {
            TransferCompletedEvent event = objectMapper.readValue(message, TransferCompletedEvent.class);

            System.out.println("Transfer completed");
            System.out.println("Transaction: " + event.transactionId());
            System.out.println("From: " + event.fromAccountId());
            System.out.println("To: " + event.toAccountId());
            System.out.println("Amount: " + event.amount());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
