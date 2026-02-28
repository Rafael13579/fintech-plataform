package com.fintech.account.dto;

import com.fintech.account.model.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "DTO de resposta da conta")
public record AccountResponseDto(

        @Schema(description = "Identificador único da conta", example = "1")
        Long id,

        @Schema(description = "Documento do titular (CPF/CNPJ)", example = "12345678900")
        String document,

        @Schema(description = "Nome do titular da conta", example = "Rafael Fernandes")
        String holderName,

        @Schema(description = "Saldo atual da conta", example = "1500.75")
        BigDecimal balance,

        @Schema(description = "Status atual da conta", example = "ACTIVE")
        AccountStatus status

) {}