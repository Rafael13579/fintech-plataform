package com.fintech.account.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record TransferRequestDto(
        @NotBlank(message = "Sender document is required")
        String fromDocument,

        @NotBlank(message = "Receiver document is required")
        String toDocument,

        @NotNull(message = "Amount is required")
        @Positive(message = "Transfer amount must be greater than zero")
        BigDecimal amount
) {}