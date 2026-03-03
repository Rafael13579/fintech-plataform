package com.fintech.account.dto;

public record TransferCompletedEvent(
        String transactionId,
        String fromAccountId,
        String toAccountId,
        String amount
) {}