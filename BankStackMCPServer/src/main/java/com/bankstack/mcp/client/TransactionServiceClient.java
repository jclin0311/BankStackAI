package com.bankstack.mcp.client;

import com.account.dto.TransactionResponse;
import com.commons.security.FeignTokenRelayConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "transaction-service-client",
        url = "${downstream.services.transaction.base-url}",
        configuration = FeignTokenRelayConfig.class
)
public interface TransactionServiceClient {

    @GetMapping("/api/v1/accounts/{accountId}/transactions")
    List<TransactionResponse> getTransactions(
            @PathVariable("accountId") UUID accountId,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Nullable OffsetDateTime startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Nullable OffsetDateTime endDate,
            @RequestParam(value = "limit", defaultValue = "5") Integer limit,
            @RequestParam(value = "offset", defaultValue = "0") Integer offset,
            @RequestParam(value = "type", required = false) @Nullable String type,
            @RequestParam(value = "amount", required = false) @Nullable java.math.BigDecimal amount
    );

    @GetMapping("/api/v1/transactions/{transactionId}")
    TransactionResponse getTransactionById(@PathVariable("transactionId") UUID transactionId);
}
