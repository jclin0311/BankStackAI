package com.bankstack.mcp.client;

import com.account.dto.AccountBalanceResponse;
import com.account.dto.AccountOwnerResponse;
import com.account.dto.AccountResponse;
import com.commons.security.FeignTokenRelayConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@FeignClient(
        name = "account-service-client",
        url = "${downstream.services.account.base-url}",
        configuration = FeignTokenRelayConfig.class
)
public interface AccountServiceClient {

    @GetMapping("/api/v1/accounts/{id}")
    AccountResponse getAccount(@PathVariable("id") UUID accountId);

    @GetMapping("/api/v1/accounts/{accountId}/balance")
    AccountBalanceResponse getAccountBalance(@PathVariable("accountId") UUID accountId);

    @GetMapping("/api/v1/accounts/{accountId}/owner")
    AccountOwnerResponse getAccountOwner(@PathVariable("accountId") UUID accountId);

    @GetMapping("/api/v1/customer/{id}/accounts")
    List<AccountResponse> getAccountsByCustomer(@PathVariable("id") String customerId);
}
