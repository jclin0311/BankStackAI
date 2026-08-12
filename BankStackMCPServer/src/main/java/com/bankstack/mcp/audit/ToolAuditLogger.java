package com.bankstack.mcp.audit;

public interface ToolAuditLogger {

    void log(ToolAuditEvent event);
}