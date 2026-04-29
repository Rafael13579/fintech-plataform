package com.notification_service.notification_service.event;

import java.math.BigDecimal;

public record TransferCompletedEvent(String transactionId,
                                     String fromAccountId,
                                     String toAccountId,
                                     BigDecimal amount) {
}
