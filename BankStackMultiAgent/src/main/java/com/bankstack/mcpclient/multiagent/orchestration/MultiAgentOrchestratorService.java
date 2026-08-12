package com.bankstack.mcpclient.multiagent.orchestration;

import com.bankstack.mcpclient.gateway.ConfirmationIntentClassifier;
import com.bankstack.mcpclient.gateway.ConversationContext;
import com.bankstack.mcpclient.gateway.ConversationContextStore;
import com.bankstack.mcpclient.guardrails.WorkflowGuardrailService;
import com.commons.security.context.UserSessionKeyResolver;
import com.bankstack.mcpclient.multiagent.agents.AgentExecutionResult;
import com.bankstack.mcpclient.multiagent.agents.AgentRequest;
import com.bankstack.mcpclient.multiagent.agents.DomainAgent;
import com.bankstack.mcpclient.multiagent.audit.MultiAgentAuditService;
import com.bankstack.mcpclient.multiagent.router.AgentType;
import com.bankstack.mcpclient.multiagent.router.MultiAgentRouter;
import com.bankstack.mcpclient.multiagent.router.RoutedTask;
import com.bankstack.mcpclient.multiagent.router.RoutingPlan;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MultiAgentOrchestratorService {

    private final MultiAgentRouter router;
    private final DomainAgentRegistry agentRegistry;
    private final MultiAgentResponseComposer responseComposer;
    private final UserSessionKeyResolver userSessionKeyResolver;
    private final ConversationContextStore contextStore;
    private final ConfirmationIntentClassifier confirmationIntentClassifier;
    private final MultiAgentAuditService auditService;
    private final WorkflowGuardrailService workflowGuardrailService;

    public MultiAgentOrchestratorService(MultiAgentRouter router,
                                         DomainAgentRegistry agentRegistry,
                                         MultiAgentResponseComposer responseComposer,
                                         UserSessionKeyResolver userSessionKeyResolver,
                                         ConversationContextStore contextStore,
                                         ConfirmationIntentClassifier confirmationIntentClassifier,
                                         MultiAgentAuditService auditService,
                                         WorkflowGuardrailService workflowGuardrailService) {
        this.router = router;
        this.agentRegistry = agentRegistry;
        this.responseComposer = responseComposer;
        this.userSessionKeyResolver = userSessionKeyResolver;
        this.contextStore = contextStore;
        this.confirmationIntentClassifier = confirmationIntentClassifier;
        this.auditService = auditService;
        this.workflowGuardrailService = workflowGuardrailService;
    }

    public MultiAgentExecutionResponse execute(String message) {
        String sessionKey = userSessionKeyResolver.resolve();
        workflowGuardrailService.validateInput(sessionKey, message);
        ConversationContext context = contextStore.get(sessionKey);
        boolean explicitConfirmation = confirmationIntentClassifier.isExplicitConfirmation(message);

        RoutingPlan plan = explicitConfirmation && context.hasPreparedAction()
                ? confirmationPlan(message)
                : router.route(message);

        auditService.routerDecision(sessionKey, plan);

        List<AgentExecutionResult> results = new ArrayList<>();
        for (RoutedTask task : plan.tasks()) {
            DomainAgent agent = agentRegistry.get(task.agentType());
            AgentExecutionResult result = agent.execute(new AgentRequest(
                    sessionKey,
                    message,
                    task,
                    context,
                    explicitConfirmation
            ));
            results.add(result);
            auditService.agentCompleted(sessionKey, result);
        }

        String answer = responseComposer.compose(plan, results);
        answer = workflowGuardrailService.sanitizeResponse(sessionKey, answer);
        String mode = plan.multiDomain() ? "MULTI_AGENT" : "SINGLE_DOMAIN_AGENT";
        auditService.finalResponse(sessionKey, mode, answer);

        boolean confirmationRequired = results.stream()
                .anyMatch(result -> result.confirmationRequired() || result.prepared());

        return new MultiAgentExecutionResponse(
                answer,
                mode,
                plan.multiDomain(),
                plan.taskCount(),
                results.size(),
                confirmationRequired,
                plan,
                List.copyOf(results)
        );
    }

    private RoutingPlan confirmationPlan(String message) {
        RoutedTask task = new RoutedTask(
                UUID.randomUUID().toString(),
                AgentType.PAYMENT,
                "CONFIRM_BILL_PAYMENT",
                message,
                true,
                "confirmBillPay",
                false,
                "Explicit confirmation must be handled only by Payment Agent."
        );
        return RoutingPlan.of(List.of(task));
    }
}
