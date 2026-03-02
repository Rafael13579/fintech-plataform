package com.fintech.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "DTO para transferência entre contas")
public record TransferRequestDto(
        @Schema(
                description = "Documento da conta de origem",
                example = "98765432106",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Orige document is required")
        UUID fromAccountId,

        @Schema(
                description = "Documento da conta de destino",
                example = "98765432100",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Receiver document is required")
        UUID toAccountId,

        @NotNull(message = "Amount is required")
        @Positive(message = "Transfer amount must be greater than zero")
        BigDecimal amount
) {}