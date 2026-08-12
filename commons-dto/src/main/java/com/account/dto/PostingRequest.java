package com.account.dto;


import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PostingRequest(
        @NotNull @Positive BigDecimal amount,
        String reason
) {}