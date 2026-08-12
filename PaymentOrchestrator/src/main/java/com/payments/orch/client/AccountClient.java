package com.payments.orch.client;

import java.util.UUID;

import com.account.dto.AccountResponse;
import com.account.dto.CreateHoldRequest;
import com.account.dto.HoldResponse;
import com.account.dto.PostingRequest;
import com.commons.security.FeignTokenRelayConfig;

import jakarta.validation.Valid;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(
        name = "account-service",
        url = "${account.service.url}",         
        configuration = FeignTokenRelayConfig.class  
)
public interface AccountClient {

    @PostMapping("/api/v1/accounts/{id}/holds")
    HoldResponse placeHold(
            @PathVariable("id") UUID id,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreateHoldRequest request
    );

   
    
}
