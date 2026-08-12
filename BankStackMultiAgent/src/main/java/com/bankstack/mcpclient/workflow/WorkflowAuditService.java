package com.bankstack.mcpclient.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class WorkflowAuditService {

    private static final Logger log = LoggerFactory.getLogger(WorkflowAuditService.class);

    public void workflowStarted(String sessionKey, WorkflowPlan plan) {
        log.info("workflow_started sessionKey={} workflowType={} summary={}", sessionKey, plan.type(), plan.summary());
    }

    public void workflowStepStarted(String sessionKey, WorkflowPlan plan, WorkflowStep step) {
        log.info("workflow_step_started sessionKey={} workflowType={} stepType={} stepName={} tool={}",
                sessionKey, plan.type(), step.type(), step.name(), step.toolName());
    }

    public void workflowStepCompleted(String sessionKey, WorkflowPlan plan, WorkflowStep step) {
        log.info("workflow_step_completed sessionKey={} workflowType={} stepType={} stepName={} tool={}",
                sessionKey, plan.type(), step.type(), step.name(), step.toolName());
    }

    public void workflowWaitingForConfirmation(String sessionKey, WorkflowPlan plan) {
        log.info("workflow_waiting_for_confirmation sessionKey={} workflowType={}", sessionKey, plan.type());
    }

    public void workflowCompleted(String sessionKey, WorkflowPlan plan, int stepsExecuted, boolean fallbackUsed) {
        log.info("workflow_completed sessionKey={} workflowType={} stepsExecuted={} fallbackUsed={}",
                sessionKey, plan.type(), stepsExecuted, fallbackUsed);
    }

    public void guardrailAllowed(String sessionKey, String checkpoint) {
        log.info("workflow_guardrail_allowed sessionKey={} checkpoint={}", sessionKey, checkpoint);
    }

    public void guardrailBlocked(String sessionKey,
                                 String checkpoint,
                                 String riskLevel,
                                 String reason) {
        log.warn("workflow_guardrail_blocked sessionKey={} checkpoint={} riskLevel={} reason={}",
                sessionKey, checkpoint, riskLevel, reason);
    }

    public void responseRedacted(String sessionKey) {
        log.info("workflow_response_redacted sessionKey={} responseSurface={}", sessionKey);
    }
}
