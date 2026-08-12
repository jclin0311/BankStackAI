package com.bankstack.mcpclient.guardrails;

import org.springframework.stereotype.Component;

/**
 * Phase 8 checkpoint 4: output guardrail.
 *
 * Tools may return internal fields or identifiers. The response shown to the
 * user should be minimized and redacted before leaving the agent gateway.
 */
@Component
public class OutputGuardrail {

    private final ResponseRedactionService responseRedactionService;

    public OutputGuardrail(ResponseRedactionService responseRedactionService) {
        this.responseRedactionService = responseRedactionService;
    }

    public String sanitize(String response) {
        return responseRedactionService.redact(response);
    }
}
