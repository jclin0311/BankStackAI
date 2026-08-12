package com.bankstack.mcp.client;

import com.bankstack.mcp.dto.PaymentView;
import com.billpay.dto.BillPayRequest;
import com.billpay.dto.PaymentAcceptedResponse;
import com.commons.security.FeignTokenRelayConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(
        name = "payment-orchestrator-client",
        url = "${downstream.services.payment.base-url}",
        configuration = FeignTokenRelayConfig.class
)
public interface PaymentOrchestratorClient {

    @PostMapping("/api/v1/payments/billpay")
    PaymentAcceptedResponse initiateBillPay(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody BillPayRequest request
    );

    @GetMapping("/api/v1/payments/{paymentId}")
    PaymentView getPayment(@PathVariable("paymentId") UUID paymentId);
}
