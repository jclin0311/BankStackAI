package com.account.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record AccountRequest(

        @NotBlank(message = "customerId is required and cannot be blank")
        String customerId,

        @NotNull(message = "accountType must be provided (e.g., CHECKING, SAVINGS)")
        AccountType accountType,

        @NotNull(message = "accountSubType must be provided (e.g., PERSONAL, BUSINESS)")
        AccountSubType accountSubType,

        @NotNull(message = "status must be provided (e.g., ACTIVE, CLOSED, PENDING)")
        AccountStatus status,

        @NotBlank(message = "currency is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "currency must follow ISO-4217 format (e.g., CAD, USD)")
        String currency,

        @Size(max = 64, message = "nickname cannot exceed 64 characters")
        String nickname,

        @Size(max = 64, message = "displayName cannot exceed 64 characters")
        String displayName,

        @PositiveOrZero(message = "openingBalance cannot be negative")
        BigDecimal openingBalance
) {}
