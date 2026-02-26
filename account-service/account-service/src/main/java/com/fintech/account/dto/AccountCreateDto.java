package com.fintech.account.dto;

import com.fintech.account.model.AccountStatus;

import java.math.BigDecimal;

public record AccountCreateDto(String document,
                               BigDecimal balance,
                               String HolderName,
                               AccountStatus status) {
}
