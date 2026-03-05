package com.notification_service.notification_service.dto;

import java.math.BigDecimal;

public record TransferCompletedEvent(String transactionId,
                                     String fromAccountId,
                                     String toAccountID,
                                     BigDecimal amount) {
}
