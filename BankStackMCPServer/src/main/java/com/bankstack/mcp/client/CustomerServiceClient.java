package com.bankstack.mcp.client;

import com.account.dto.CustomerResponse;
import com.commons.security.FeignTokenRelayConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "customer-service-client",
        url = "${downstream.services.customer.base-url}",
        configuration = FeignTokenRelayConfig.class
)
public interface CustomerServiceClient {

    @GetMapping("/api/v1/customers/{externalId}")
    CustomerResponse getCustomerByExternalId(@PathVariable("externalId") String externalId);

    @GetMapping("/api/v1/customers/{externalId}/exists")
    Boolean existsByExternalId(@PathVariable("externalId") String externalId);
}
