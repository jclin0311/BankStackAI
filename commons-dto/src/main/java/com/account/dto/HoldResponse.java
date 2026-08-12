package com.account.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record HoldResponse(
        UUID holdId,
        BigDecimal amount,
        HoldStatus status,
        LocalDateTime createdAt,
        LocalDateTime releaseAt
) {}