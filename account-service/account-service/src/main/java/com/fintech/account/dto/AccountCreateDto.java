package com.fintech.account.dto;

import com.fintech.account.model.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(description = "DTO para criação de conta")
public record AccountCreateDto(

        @Schema(
                description = "Documento do titular (CPF ou CNPJ)",
                example = "12345678900",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Document is required")
        @Size(min = 11, max = 14, message = "Document must be between 11 and 14 characters")
        String document,

        @Schema(
                description = "Nome do titular da conta",
                example = "Rafael Silva",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Holder name is required")
        @Size(min = 3, max = 100, message = "Holder name must be between 3 and 100 characters")
        String holderName,

        @Schema(
                description = "Saldo inicial da conta",
                example = "0.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Initial balance cannot be null")
        @PositiveOrZero(message = "Initial balance cannot be negative")
        BigDecimal balance

) {}