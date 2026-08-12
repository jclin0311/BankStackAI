package com.bankstack.mcp.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class StructuredToolAuditLogger implements ToolAuditLogger {

    private static final Logger log = LoggerFactory.getLogger("TOOL_AUDIT");

    @Override
    public void log(ToolAuditEvent event) {
        log.info(
                "tool_audit timestamp={} tool={} actorId={} subject={} customerExternalId={} role={} cid={} confirmationPresent={} otpVerified={} outcome={} requestSummary=\"{}\" resultSummary=\"{}\" errorMessage=\"{}\"",
                event.timestamp(),
                event.toolName(),
                safe(event.actorId()),
                safe(event.subject()),
                safe(event.customerExternalId()),
                event.actorRole(),
                safe(event.correlationId()),
                event.confirmationPresent(),
                event.otpVerified(),
                event.outcome(),
                safe(event.requestSummary()),
                safe(event.resultSummary()),
                safe(event.errorMessage())
        );
    }

    private String safe(String value) {
        return value == null ? "" : value.replace("\"", "'");
    }
}