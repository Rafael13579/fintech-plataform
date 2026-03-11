package com.fintech.account.event;

public record TransferCompletedEvent(
        String transactionId,
        String fromAccountId,
        String toAccountId,
        String amount
) {}