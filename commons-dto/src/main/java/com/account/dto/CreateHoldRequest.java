package com.account.dto;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;


public record CreateHoldRequest(
        @NotNull @Positive BigDecimal amount,
        String reason,
        LocalDateTime releaseAt,
        String idempotencyKey
) {
    public CreateHoldRequest withIdempotencyKey(String k) {
        return new CreateHoldRequest(this.amount(), this.reason(), this.releaseAt(), k);
    }
}