package com.fintech.account.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record AccountCreateDto(
        @NotBlank(message = "Document is required")
        @Size(min = 11, max = 14, message = "Document must be between 11 and 14 characters")
        String document,

        @NotBlank(message = "Holder name is required")
        @Size(min = 3, max = 100, message = "Holder name must be between 3 and 100 characters")
        String holderName,

        @NotNull(message = "Initial balance cannot be null")
        @PositiveOrZero(message = "Initial balance cannot be negative")
        BigDecimal balance

) {}