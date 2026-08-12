package com.bankstack.mcpclient.multiagent.agents;

import com.bankstack.mcpclient.gateway.ConversationContext;
import com.bankstack.mcpclient.gateway.McpToolExecutionService;
import com.bankstack.mcpclient.gateway.ConversationContextStore;
import com.bankstack.mcpclient.gateway.PreparedActionType;
import com.bankstack.mcpclient.gateway.ToolArgumentContractValidator;
import com.bankstack.mcpclient.gateway.ToolArgumentExtractionService;
import com.bankstack.mcpclient.gateway.ToolArgumentNormalizer;
import com.commons.dto.ToolExecutionResult;
import com.bankstack.mcpclient.gateway.ToolGatewayPolicyService;
import com.bankstack.mcpclient.gateway.ToolResponsePresenter;
import com.commons.exception.GuardrailViolationException;
import com.bankstack.mcpclient.guardrails.WorkflowGuardrailService;
import com.bankstack.mcpclient.multiagent.audit.MultiAgentAuditService;
import com.bankstack.mcpclient.memory.ActionMemoryService;
import com.bankstack.mcpclient.multiagent.router.AgentType;
import com.bankstack.mcpclient.multiagent.router.RoutedTask;
import com.bankstack.mcpclient.multiagent.tools.ScopedToolRegistry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractToolBackedDomainAgent implements DomainAgent {

    private final ScopedToolRegistry scopedToolRegistry;
    private final ToolArgumentExtractionService extractionService;
    private final ToolArgumentNormalizer normalizer;
    private final ToolArgumentContractValidator contractValidator;
    private final WorkflowGuardrailService workflowGuardrailService;
    private final McpToolExecutionService mcpToolExecutionService;
    private final ToolResponsePresenter toolResponsePresenter;
    private final ConversationContextStore contextStore;
    private final ActionMemoryService actionMemoryService;
    private final ToolGatewayPolicyService policyService;
    private final MultiAgentAuditService auditService;

    protected AbstractToolBackedDomainAgent(ScopedToolRegistry scopedToolRegistry,
                                            ToolArgumentExtractionService extractionService,
                                            ToolArgumentNormalizer normalizer,
                                            ToolArgumentContractValidator contractValidator,
                                            WorkflowGuardrailService workflowGuardrailService,
                                            McpToolExecutionService mcpToolExecutionService,
                                            ToolResponsePresenter toolResponsePresenter,
                                            ConversationContextStore contextStore,
                                            ActionMemoryService actionMemoryService,
                                            ToolGatewayPolicyService policyService,
                                            MultiAgentAuditService auditService) {
        this.scopedToolRegistry = scopedToolRegistry;
        this.extractionService = extractionService;
        this.normalizer = normalizer;
        this.contractValidator = contractValidator;
        this.workflowGuardrailService = workflowGuardrailService;
        this.mcpToolExecutionService = mcpToolExecutionService;
        this.toolResponsePresenter = toolResponsePresenter;
        this.contextStore = contextStore;
        this.actionMemoryService = actionMemoryService;
        this.policyService = policyService;
        this.auditService = auditService;
    }

    @Override
    public AgentExecutionResult execute(AgentRequest request) {
        RoutedTask task = request.task();
        String toolName = selectTool(request);

        if (toolName == null || toolName.isBlank()) {
            return AgentExecutionResult.skipped(
                    supports(),
                    agentName(),
                    task.taskId(),
                    task.intent(),
                    noToolMessage(request)
            );
        }

        auditService.agentSelected(request.sessionKey(), task.taskId(), agentName(), task.intent(), toolName);

        if (!scopedToolRegistry.isAllowed(supports(), toolName)) {
            return result(
                    request,
                    toolName,
                    AgentExecutionStatus.BLOCKED,
                    "Blocked by scoped tools. " + agentName() + " is not allowed to call " + toolName + ".",
                    List.of(),
                    false,
                    null,
                    Map.of(),
                    null,
                    List.of("SCOPED_TOOL_DENY")
            );
        }

        Map<String, Object> arguments = buildArguments(request, toolName);

        try {
            workflowGuardrailService.validateToolAttempt(
                    request.sessionKey(),
                    toolName,
                    request.message(),
                    request.explicitConfirmation(),
                    removeInternalArguments(arguments)
            );
        } catch (GuardrailViolationException ex) {
            return result(
                    request,
                    toolName,
                    AgentExecutionStatus.BLOCKED,
                    ex.getMessage(),
                    List.of(),
                    false,
                    null,
                    arguments,
                    null,
                    List.of(ex.checkName() + ":" + ex.riskLevel())
            );
        }

        ToolExecutionResult validation = contractValidator.validate(toolName, arguments);
        if (validation.needsInput() || validation.failed()) {
            if (validation.needsInput()) {
                contextStore.saveAwaitingTool(
                        request.sessionKey(),
                        toolName,
                        validation.getMissingFields(),
                        arguments
                );
            }

            return result(
                    request,
                    toolName,
                    validation.failed() ? AgentExecutionStatus.FAILED : AgentExecutionStatus.NEEDS_INPUT,
                    validation.getMessage(),
                    validation.getMissingFields(),
                    false,
                    null,
                    arguments,
                    validation,
                    List.of()
            );
        }

        ToolExecutionResult toolResult = mcpToolExecutionService.execute(toolName, removeInternalArguments(arguments));
        String presented = toolResponsePresenter.present(toolName, toolResult);
        presented = workflowGuardrailService.sanitizeResponse(request.sessionKey(), presented);
        AgentExecutionStatus status = mapStatus(toolResult);

        if (toolResult.prepared() || toolResult.isRequiresConfirmation()) {
            rememberPreparedAction(request.sessionKey(), toolName, presented, toolResult);
        } else if (toolResult.executed()) {
            rememberSuccessfulAction(request.sessionKey(), supports(), toolName, task.intent(), arguments);
        }

        return result(
                request,
                toolName,
                status,
                presented,
                toolResult.getMissingFields(),
                toolResult.isRequiresConfirmation(),
                toolResult.getConfirmationToken(),
                arguments,
                toolResult,
                List.of()
        );
    }

    protected abstract String selectTool(AgentRequest request);

    protected String noToolMessage(AgentRequest request) {
        return agentName() + " understood the task, but no governed tool is required yet.";
    }

    protected Map<String, Object> buildArguments(AgentRequest request, String toolName) {
        Map<String, Object> input = new HashMap<>();
        ConversationContext context = request.conversationContext();

        if (request.explicitConfirmation()
                && "confirmBillPay".equals(toolName)
                && context != null
                && context.hasPreparedAction()) {
            input.put("confirmationToken", context.confirmationToken());
            if (context.preparedActionData() != null) {
                input.putAll(context.preparedActionData());
            }
        } else {
            if (context != null
                    && context.isAwaitingInput()
                    && toolName.equals(context.awaitingTool())
                    && context.awaitingArgumentData() != null) {
                input.putAll(context.awaitingArgumentData());
            }
            input.putAll(extractionService.extract(toolName, request.message(), context));
        }

       
       

        return normalizer.normalize(toolName, request.message(), input);
    }

    private void rememberPreparedAction(String sessionKey,
                                        String toolName,
                                        String safeAnswer,
                                        ToolExecutionResult result) {
        String token = result.getConfirmationToken();
        if (token == null || token.isBlank()) {
            return;
        }

        contextStore.savePreparedAction(
                sessionKey,
                resolvePreparedActionType(result.getActionType(), toolName),
                policyService.summarizePreparedAction(safeAnswer),
                token,
                result.getData()
        );
    }

    private void rememberSuccessfulAction(String sessionKey,
                                          AgentType agentType,
                                          String toolName,
                                          String intent,
                                          Map<String, Object> arguments) {
        if (sessionKey == null || sessionKey.isBlank()) {
            return;
        }

        if (!"prepareBillPay".equals(toolName)) {
            return;
        }

        Map<String, Object> safeMemory = new HashMap<>();
        putIfPresent(safeMemory, "debtorAccountId", arguments == null ? null : arguments.get("debtorAccountId"));
        putIfPresent(safeMemory, "billerReferenceNumber", arguments == null ? null : arguments.get("billerReferenceNumber"));
        putIfPresent(safeMemory, "currency", arguments == null ? null : arguments.get("currency"));

        if (safeMemory.isEmpty()) {
            return;
        }

        String scopedIntent = agentType == null ? intent : agentType.name() + ":" + intent;
        actionMemoryService.rememberSuccessfulAction(sessionKey, toolName, scopedIntent, safeMemory);
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null && !value.toString().isBlank()) {
            target.put(key, value);
        }
    }

    private PreparedActionType resolvePreparedActionType(String actionType, String toolName) {
        if ("BILL_PAY".equalsIgnoreCase(actionType)
                || "prepareBillPay".equals(toolName)
                || "confirmBillPay".equals(toolName)) {
            return PreparedActionType.BILL_PAY;
        }
        return PreparedActionType.NONE;
    }

    private AgentExecutionStatus mapStatus(ToolExecutionResult result) {
        if (result == null) {
            return AgentExecutionStatus.FAILED;
        }
        if (result.needsInput()) {
            return AgentExecutionStatus.NEEDS_INPUT;
        }
        if (result.prepared()) {
            return AgentExecutionStatus.PREPARED;
        }
        if (result.executed()) {
            return AgentExecutionStatus.EXECUTED;
        }
        if (result.failed()) {
            return AgentExecutionStatus.FAILED;
        }
        return AgentExecutionStatus.SUCCESS;
    }

    private AgentExecutionResult result(AgentRequest request,
                                        String toolName,
                                        AgentExecutionStatus status,
                                        String answer,
                                        List<String> missingFields,
                                        boolean confirmationRequired,
                                        String confirmationToken,
                                        Map<String, Object> arguments,
                                        ToolExecutionResult toolResult,
                                        List<String> warnings) {
        return new AgentExecutionResult(
                supports(),
                agentName(),
                request.task().taskId(),
                request.task().intent(),
                toolName,
                status,
                answer == null || answer.isBlank() ? "No response produced." : answer,
                missingFields == null ? List.of() : List.copyOf(missingFields),
                confirmationRequired,
                confirmationToken,
                arguments == null ? Map.of() : Map.copyOf(arguments),
                toolResult,
                warnings == null ? List.of() : List.copyOf(warnings)
        );
    }

    private Map<String, Object> removeInternalArguments(Map<String, Object> toolArguments) {
        Map<String, Object> cleaned = new HashMap<>(toolArguments == null ? Map.of() : toolArguments);
        cleaned.keySet().removeIf(key -> key != null && key.startsWith("_"));
        return cleaned;
    }
}
