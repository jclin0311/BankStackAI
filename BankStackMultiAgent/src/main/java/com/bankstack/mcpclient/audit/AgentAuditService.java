package com.bankstack.mcpclient.audit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentAuditService {

    private static final Logger log = LoggerFactory.getLogger(AgentAuditService.class);

    public void requestReceived(String sessionKey, String request, boolean explicitConfirmation) {
        log.info("agent_request_received sessionKey={} explicitConfirmation={} request={}",
                sessionKey, explicitConfirmation, request);
    }

    public void requestSucceeded(String sessionKey, String request, String response,  boolean explicitConfirmation) {
        log.info("agent_request_succeeded sessionKey={} explicitConfirmation={} request={} response={}",
                sessionKey, explicitConfirmation, request, response);
    }

    public void requestFailed(String sessionKey, String request,  boolean explicitConfirmation, Exception ex) {
        log.error("agent_request_failed sessionKey={} explicitConfirmation={} request={} error={}",
                sessionKey, explicitConfirmation, request, ex.getMessage(), ex);
    }

    public void securityEvent(String sessionKey,
                              String actor,
                              String checkName,
                              String outcome,
                              String riskLevel,
                              String detail) {
        log.warn("agent_security_event sessionKey={} actor={} check={} outcome={} risk={} detail={}",
                sessionKey, actor, checkName, outcome, riskLevel, detail);
    }
}