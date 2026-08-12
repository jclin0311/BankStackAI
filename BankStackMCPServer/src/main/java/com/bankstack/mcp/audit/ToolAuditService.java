package com.bankstack.mcp.audit;

import com.bankstack.mcp.safety.ToolInvocationContext;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class ToolAuditService {

    private final ToolAuditLogger toolAuditLogger;

    public ToolAuditService(ToolAuditLogger toolAuditLogger) {
        this.toolAuditLogger = toolAuditLogger;
    }

    public void success(String toolName,
                        ToolInvocationContext context,
                        String requestSummary,
                        String resultSummary) {
        toolAuditLogger.log(new ToolAuditEvent(
                OffsetDateTime.now(),
                toolName,
                context.actorId(),
                context.subject(),
                context.customerExternalId(),
                context.actorRole(),
                currentCorrelationId(),
                context.confirmationPresent(),
                context.otpVerified(),
                AuditOutcome.SUCCESS,
                requestSummary,
                resultSummary,
                null
        ));
    }

    public void failure(String toolName,
                        ToolInvocationContext context,
                        String requestSummary,
                        Exception exception) {
        toolAuditLogger.log(new ToolAuditEvent(
                OffsetDateTime.now(),
                toolName,
                context.actorId(),
                context.subject(),
                context.customerExternalId(),
                context.actorRole(),
                currentCorrelationId(),
                context.confirmationPresent(),
                context.otpVerified(),
                AuditOutcome.FAILURE,
                requestSummary,
                null,
                exception == null ? "Unknown error" : exception.getMessage()
        ));
    }

    private String currentCorrelationId() {
        String cid = MDC.get("cid");
        return cid == null ? "" : cid;
    }
}