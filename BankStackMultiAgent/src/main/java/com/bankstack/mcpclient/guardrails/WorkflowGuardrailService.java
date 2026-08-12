package com.bankstack.mcpclient.guardrails;

import com.commons.exception.GuardrailViolationException;

import com.bankstack.mcpclient.workflow.WorkflowAuditService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Phase 8 guardrail facade.
 *
 * Important boundary:
 * Agent guardrails are conversation and workflow controls.
 * Banking authorization, actor validity, ownership, and role enforcement remain
 * inside BankStackMCPServer.
 *
 * The agent keeps only three checkpoints:
 * 1. Input guardrail
 * 2. Intent/workflow guardrail
 * 3. Output redaction guardrail
 */
@Service
public class WorkflowGuardrailService {

    private final InputGuardrail inputGuardrail;
    private final ToolIntentGuardrail toolIntentGuardrail;
    private final OutputGuardrail outputGuardrail;
    private final WorkflowAuditService workflowAuditService;

    public WorkflowGuardrailService(InputGuardrail inputGuardrail,
                                    ToolIntentGuardrail toolIntentGuardrail,
                                    OutputGuardrail outputGuardrail,
                                    WorkflowAuditService workflowAuditService) {
        this.inputGuardrail = inputGuardrail;
        this.toolIntentGuardrail = toolIntentGuardrail;
        this.outputGuardrail = outputGuardrail;
        this.workflowAuditService = workflowAuditService;
    }

    public void validateInput(String sessionKey,
                              String message) {
        try {
            GuardrailDecision decision = inputGuardrail.validate(message);
            workflowAuditService.guardrailAllowed(sessionKey, decision.checkName());
        } catch (GuardrailViolationException ex) {
            workflowAuditService.guardrailBlocked(sessionKey, ex.checkName(), ex.riskLevel(), ex.getMessage());
            throw ex;
        }
    }

    public void validateToolAttempt(String sessionKey,
                                    String toolName,
                                    String message,
                                    boolean explicitConfirmation,
                                    Map<String, Object> arguments) {
        try {
            GuardrailDecision decision = toolIntentGuardrail.validate(toolName, message, explicitConfirmation, arguments);
            workflowAuditService.guardrailAllowed(sessionKey, decision.checkName());
        } catch (GuardrailViolationException ex) {
            workflowAuditService.guardrailBlocked(sessionKey, ex.checkName(), ex.riskLevel(), ex.getMessage());
            throw ex;
        }
    }

    public String sanitizeResponse(String sessionKey,
                                   String response) {
        String sanitized = outputGuardrail.sanitize(response);

        if (response != null && !response.equals(sanitized)) {
            workflowAuditService.responseRedacted(sessionKey);
        }

        return sanitized;
    }
}
