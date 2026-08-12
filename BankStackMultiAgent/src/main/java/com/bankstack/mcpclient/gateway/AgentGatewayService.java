package com.bankstack.mcpclient.gateway;

import com.commons.dto.ToolExecutionResult;

import com.bankstack.mcpclient.api.GatewayChatRequest;
import com.bankstack.mcpclient.api.GatewayChatResponse;
import com.bankstack.mcpclient.audit.AgentAuditService;
import com.commons.exception.GuardrailViolationException;
import com.bankstack.mcpclient.guardrails.WorkflowGuardrailService;
import com.bankstack.mcpclient.memory.ActionMemoryService;
import com.bankstack.mcpclient.memory.ContextRecoveryService;
import com.bankstack.mcpclient.memory.RecoveredContextSuggestion;
import com.bankstack.mcpclient.workflow.WorkflowAuditService;
import com.bankstack.mcpclient.workflow.WorkflowExecutionResult;
import com.bankstack.mcpclient.workflow.WorkflowPlan;
import com.bankstack.mcpclient.workflow.WorkflowPlanFactory;
import com.bankstack.mcpclient.workflow.WorkflowResponseComposer;
import com.bankstack.mcpclient.workflow.WorkflowStep;
import com.bankstack.mcpclient.workflow.WorkflowStepType;
import com.commons.security.context.UserSessionKeyResolver;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentGatewayService {

    private final ConfirmationIntentClassifier confirmationIntentClassifier;
    private final GatewayRequestValidator requestValidator;
    private final AgentAuditService auditService;
    private final GatewayExceptionTranslator exceptionTranslator;
    private final ConversationContextStore contextStore;
    private final ConfirmationGuardService confirmationGuard;
    private final ToolGatewayPolicyService policyService;
    private final UserSessionKeyResolver userSessionKeyResolver;
    private final RoutePolicyService routePolicyService;
    private final RefusalResponseFactory refusalResponseFactory;
    private final DirectAnswerService directAnswerService;
    private final DeterministicIntentRouter deterministicIntentRouter;
    private final AiToolIntentResolver aiToolIntentResolver;
    private final FallbackToolIntentResolver fallbackToolIntentResolver;
    private final ApprovedToolAccessGuard approvedToolAccessGuard;
    private final McpToolExecutionService mcpToolExecutionService;
    private final ToolArgumentExtractionService toolArgumentExtractionService;
    private final ToolArgumentNormalizer toolArgumentNormalizer;
    private final ToolArgumentContractValidator toolArgumentContractValidator;
    private final ToolResponsePresenter toolResponsePresenter;
    private final ContextRecoveryService contextRecoveryService;
    private final ActionMemoryService actionMemoryService;
    private final WorkflowGuardrailService workflowGuardrailService;
    private final WorkflowPlanFactory workflowPlanFactory;
    private final WorkflowResponseComposer workflowResponseComposer;
    private final WorkflowAuditService workflowAuditService;

    public AgentGatewayService(ConfirmationIntentClassifier confirmationIntentClassifier,
                               GatewayRequestValidator requestValidator,
                               AgentAuditService auditService,
                               GatewayExceptionTranslator exceptionTranslator,
                               ConversationContextStore contextStore,
                               ConfirmationGuardService confirmationGuard,
                               ToolGatewayPolicyService policyService,
                               UserSessionKeyResolver userSessionKeyResolver,
                               RoutePolicyService routePolicyService,
                               RefusalResponseFactory refusalResponseFactory,
                               DirectAnswerService directAnswerService,
                               DeterministicIntentRouter deterministicIntentRouter,
                               AiToolIntentResolver aiToolIntentResolver,
                               FallbackToolIntentResolver fallbackToolIntentResolver,
                               ApprovedToolAccessGuard approvedToolAccessGuard,
                               McpToolExecutionService mcpToolExecutionService,
                               ToolArgumentExtractionService toolArgumentExtractionService,
                               ToolArgumentNormalizer toolArgumentNormalizer,
                               ToolArgumentContractValidator toolArgumentContractValidator,
                               ToolResponsePresenter toolResponsePresenter,
                               ContextRecoveryService contextRecoveryService,
                               ActionMemoryService actionMemoryService,
                               WorkflowGuardrailService workflowGuardrailService,
                               WorkflowPlanFactory workflowPlanFactory,
                               WorkflowResponseComposer workflowResponseComposer,
                               WorkflowAuditService workflowAuditService) {

        this.confirmationIntentClassifier = confirmationIntentClassifier;
        this.requestValidator = requestValidator;
        this.auditService = auditService;
        this.exceptionTranslator = exceptionTranslator;
        this.contextStore = contextStore;
        this.confirmationGuard = confirmationGuard;
        this.policyService = policyService;
        this.userSessionKeyResolver = userSessionKeyResolver;
        this.routePolicyService = routePolicyService;
        this.refusalResponseFactory = refusalResponseFactory;
        this.directAnswerService = directAnswerService;
        this.deterministicIntentRouter = deterministicIntentRouter;
        this.aiToolIntentResolver = aiToolIntentResolver;
        this.fallbackToolIntentResolver = fallbackToolIntentResolver;
        this.approvedToolAccessGuard = approvedToolAccessGuard;
        this.mcpToolExecutionService = mcpToolExecutionService;
        this.toolArgumentExtractionService = toolArgumentExtractionService;
        this.toolArgumentNormalizer = toolArgumentNormalizer;
        this.toolArgumentContractValidator = toolArgumentContractValidator;
        this.toolResponsePresenter = toolResponsePresenter;
        this.contextRecoveryService = contextRecoveryService;
        this.actionMemoryService = actionMemoryService;
        this.workflowGuardrailService = workflowGuardrailService;
        this.workflowPlanFactory = workflowPlanFactory;
        this.workflowResponseComposer = workflowResponseComposer;
        this.workflowAuditService = workflowAuditService;
    }

    public GatewayChatResponse handle(GatewayChatRequest request) {

        requestValidator.validate(request);

        String sessionKey = userSessionKeyResolver.resolve();

        workflowGuardrailService.validateInput(
                sessionKey,
                request.message()
        );

        boolean explicitConfirmation =
                confirmationIntentClassifier.isExplicitConfirmation(
                        request.message()
                );

        ConversationContext context = contextStore.get(sessionKey);

        Route route =
                routePolicyService.classify(
                        request.message(),
                        explicitConfirmation,
                        context
                );


        auditService.requestReceived(
                sessionKey,
                request.message(),
                explicitConfirmation
        );

        try {

            WorkflowExecutionResult workflowResult = null;
            ToolRouteOutcome toolRouteOutcome = null;
            String answer;

            if (route == Route.REFUSE) {
                answer = refusalResponseFactory.create(request.message());
            } else if (route == Route.DIRECT) {
                answer = directAnswerService.answer(request.message());
            } else if (route == Route.TOOL) {
                toolRouteOutcome = handleToolRouteOutcome(
                        sessionKey,
                        request.message(),
                        explicitConfirmation,
                        context,
                        null
                );
                answer = toolRouteOutcome.answer();
            } else if (route == Route.WORKFLOW) {
                workflowResult = handleWorkflowRoute(
                        sessionKey,
                        request.message(),
                        explicitConfirmation,
                        context
                );
                answer = workflowResult.answer();
            } else {
                answer = directAnswerService.answer(request.message());
            }

            String safeAnswer = answer == null ? "" : answer;
            safeAnswer = workflowGuardrailService.sanitizeResponse(
                    sessionKey,
                    safeAnswer
            );

            auditService.requestSucceeded(
                    sessionKey,
                    request.message(),
                    safeAnswer,
                    explicitConfirmation
            );

            if (workflowResult != null) {
                return new GatewayChatResponse(
                        safeAnswer,
                        explicitConfirmation,
                        route.name(),
                        null,
                        null,
                        null,
                        true,
                        workflowResult.workflowType().name(),
                        workflowResult.planSummary(),
                        workflowResult.stepsExecuted(),
                        workflowResult.fallbackUsed()
                );
            }

            if (toolRouteOutcome != null) {
                return new GatewayChatResponse(
                        safeAnswer,
                        explicitConfirmation,
                        route.name(),
                        toolRouteOutcome.toolName(),
                        toolRouteOutcome.toolArguments(),
                        toolRouteOutcome.toolResult()
                );
            }

            return new GatewayChatResponse(
                    safeAnswer,
                    explicitConfirmation,
                    route.name()
            );

        } catch (GuardrailViolationException ex) {

            auditService.requestFailed(
                    sessionKey,
                    request.message(),
                    explicitConfirmation,
                    ex
            );

            throw ex;

        } catch (Exception ex) {

            auditService.requestFailed(
                    sessionKey,
                    request.message(),
                    explicitConfirmation,
                    ex
            );

            throw exceptionTranslator.translate(ex);
        }
    }

    private WorkflowExecutionResult handleWorkflowRoute(String sessionKey,
                                                        String message,
                                                        boolean explicitConfirmation,
                                                        ConversationContext context) {

        WorkflowPlan plan = workflowPlanFactory.create(message);
        workflowAuditService.workflowStarted(sessionKey, plan);

        java.util.List<String> outputs = new java.util.ArrayList<>();
        int stepsExecuted = 0;
        boolean fallbackUsed = false;

        for (WorkflowStep step : plan.steps()) {

            workflowAuditService.workflowStepStarted(sessionKey, plan, step);

            if (step.type() == WorkflowStepType.TOOL) {

                if (explicitConfirmation
                        && context != null
                        && context.hasPreparedAction()
                        && "prepareBillPay".equals(step.toolName())) {
                    workflowAuditService.workflowStepCompleted(sessionKey, plan, step);
                    continue;
                }

                String toolOutput = handleToolRoute(
                        sessionKey,
                        message,
                        explicitConfirmation,
                        contextStore.get(sessionKey),
                        step.toolName()
                );

                outputs.add(step.name() + ": " + toolOutput);
                stepsExecuted++;

                if (step.retryable() && looksTransientFailure(toolOutput)) {
                    fallbackUsed = true;
                    String retryOutput = handleToolRoute(
                            sessionKey,
                            message,
                            explicitConfirmation,
                            contextStore.get(sessionKey),
                            step.toolName()
                    );
                    outputs.add(step.name() + " retry: " + retryOutput);
                    stepsExecuted++;
                }

                workflowAuditService.workflowStepCompleted(sessionKey, plan, step);
                continue;
            }

            if (step.type() == WorkflowStepType.CONFIRM
                    && plan.requiresConfirmation()
                    && !explicitConfirmation) {

                workflowAuditService.workflowWaitingForConfirmation(sessionKey, plan);

                String answer = workflowResponseComposer.compose(
                        plan,
                        outputs,
                        true,
                        fallbackUsed
                );

                return new WorkflowExecutionResult(
                        answer,
                        plan.type(),
                        plan.summary(),
                        stepsExecuted,
                        fallbackUsed,
                        true
                );
            }

            stepsExecuted++;
            workflowAuditService.workflowStepCompleted(sessionKey, plan, step);
        }

        workflowAuditService.workflowCompleted(sessionKey, plan, stepsExecuted, fallbackUsed);

        String answer = workflowResponseComposer.compose(
                plan,
                outputs,
                false,
                fallbackUsed
        );

        return new WorkflowExecutionResult(
                answer,
                plan.type(),
                plan.summary(),
                stepsExecuted,
                fallbackUsed,
                false
        );
    }

    private boolean looksTransientFailure(String output) {
        if (output == null) {
            return false;
        }
        String value = output.toLowerCase();
        return value.contains("timeout")
                || value.contains("temporarily unavailable")
                || value.contains("try again")
                || value.contains("connection")
                || value.contains("503")
                || value.contains("504");
    }

    private String handleToolRoute(String sessionKey,
                                   String message,
                                   boolean explicitConfirmation,
                                   ConversationContext context) {
        return handleToolRouteOutcome(sessionKey, message, explicitConfirmation, context, null).answer();
    }

    private String handleToolRoute(String sessionKey,
                                   String message,
                                   boolean explicitConfirmation,
                                   ConversationContext context,
                                   String forcedToolName) {
        return handleToolRouteOutcome(sessionKey, message, explicitConfirmation, context, forcedToolName).answer();
    }

    private ToolRouteOutcome handleToolRouteOutcome(String sessionKey,
                                                   String message,
                                                   boolean explicitConfirmation,
                                                   ConversationContext context,
                                                   String forcedToolName) {

        boolean explicitRejection =
                isExplicitRejection(message);

        /*
         * User rejected recovered memory suggestion.
         * This must be handled before confirmationGuard.
         */
        if (context != null
                && context.awaitingRecoveredContextConfirmation()
                && explicitRejection) {

            contextStore.clearRecoveredSuggestion(sessionKey);

            ToolExecutionResult rejectionResult = ToolExecutionResult.needInput(
                    "Please provide the bill payment details manually.",
                    context.missingFields(),
                    actionTypeForTool(context.awaitingTool())
            );

            return new ToolRouteOutcome(
                    """
                    No problem.
                    Please provide the bill payment details manually.
                    """,
                    context.awaitingTool(),
                    sanitizeArgumentsForResponse(context.awaitingArgumentData()),
                    rejectionResult
            );
        }

        /*
         * User accepted recovered memory suggestion.
         * This is NOT payment confirmation.
         * It only means: use recovered DB details.
         */
        if (context != null
                && context.awaitingRecoveredContextConfirmation()
                && explicitConfirmation) {

            contextStore.mergeRecoveredArguments(sessionKey);

            context = contextStore.get(sessionKey);

            explicitConfirmation = false;
        }

        confirmationGuard.validateConfirmation(
                explicitConfirmation,
                context
        );

        String intendedTool = forcedToolName == null || forcedToolName.isBlank()
                ? resolveIntendedTool(
                        message,
                        explicitConfirmation,
                        context
                )
                : forcedToolName;

        approvedToolAccessGuard.validate(intendedTool);

        ConversationContext effectiveContext = context;

        if (isSwitchingAwayFromAwaitingTool(
                context,
                intendedTool
        )) {

            contextStore.clearAwaiting(sessionKey);

            effectiveContext = ConversationContext.empty();
        }

        Map<String, Object> toolArguments =
                buildToolArguments(
                        intendedTool,
                        message,
                        effectiveContext,
                        sessionKey
                );

        toolArguments =
                toolArgumentNormalizer.normalize(
                        intendedTool,
                        message,
                        toolArguments
                );

        ToolExecutionResult validationResult =
                toolArgumentContractValidator.validate(
                        intendedTool,
                        toolArguments
                );

        if (validationResult.needsInput()) {

            RecoveredContextSuggestion suggestion =
                    contextRecoveryService.recover(
                            sessionKey,
                            intendedTool,
                            toolArguments
                    );

            if (suggestion.found()) {

                contextStore.saveRecoveredSuggestion(
                        sessionKey,
                        intendedTool,
                        validationResult.getMissingFields(),
                        toolArguments,
                        suggestion.recoveredArguments()
                );

                List<String> remainingMissingFields = remainingMissingFields(
                        validationResult.getMissingFields(),
                        suggestion.recoveredArguments()
                );

                ToolExecutionResult recoveredResult = ToolExecutionResult.needInput(
                        "Recovered previous bill payment details. Awaiting user approval before applying them.",
                        remainingMissingFields,
                        validationResult.getActionType()
                );

                recoveredResult.setData(Map.of(
                        "recoveredSuggestion",
                        recoveredSuggestionForResponse(suggestion.recoveredArguments())
                ));

                return new ToolRouteOutcome(
                        suggestion.message(),
                        intendedTool,
                        sanitizeArgumentsForResponse(toolArguments),
                        recoveredResult
                );
            }

            contextStore.saveAwaitingTool(
                    sessionKey,
                    intendedTool,
                    validationResult.getMissingFields(),
                    toolArguments
            );

            return new ToolRouteOutcome(
                    validationResult.getMessage(),
                    intendedTool,
                    sanitizeArgumentsForResponse(toolArguments),
                    validationResult
            );
        }

        if (validationResult.failed()) {
            return new ToolRouteOutcome(
                    validationResult.getMessage(),
                    intendedTool,
                    sanitizeArgumentsForResponse(toolArguments),
                    validationResult
            );
        }

        Map<String, Object> executableArguments =
                removeInternalArguments(toolArguments);

        workflowGuardrailService.validateToolAttempt(
                sessionKey,
                intendedTool,
                message,
                explicitConfirmation,
                executableArguments
        );

        ToolExecutionResult result =
                mcpToolExecutionService.execute(
                        intendedTool,
                        executableArguments
                );

        String responseMessage =
                toolResponsePresenter.present(
                        intendedTool,
                        result
                );

        responseMessage = workflowGuardrailService.sanitizeResponse(
                sessionKey,
                responseMessage
        );

        if (result.failed()) {
            return new ToolRouteOutcome(
                    responseMessage,
                    intendedTool,
                    sanitizeArgumentsForResponse(executableArguments),
                    result
            );
        }

        if (result.needsInput()) {

            contextStore.saveAwaitingTool(
                    sessionKey,
                    intendedTool,
                    result.getMissingFields(),
                    toolArguments
            );

            return new ToolRouteOutcome(
                    responseMessage,
                    intendedTool,
                    sanitizeArgumentsForResponse(executableArguments),
                    result
            );
        }

        rememberSuccessfulToolAction(
                sessionKey,
                intendedTool,
                toolArguments,
                result
        );

        contextStore.clearAwaiting(sessionKey);

        updateConversationContextAfterToolExecution(
                sessionKey,
                explicitConfirmation,
                intendedTool,
                result,
                responseMessage
        );

        return new ToolRouteOutcome(
                responseMessage,
                intendedTool,
                sanitizeArgumentsForResponse(executableArguments),
                result
        );
    }

    private String resolveIntendedTool(String message,
                                       boolean explicitConfirmation,
                                       ConversationContext context) {

        AgentDecision deterministicTool =
                deterministicIntentRouter.route(
                        message,
                        explicitConfirmation,
                        context
                );

        if (deterministicTool != null) {
            return deterministicTool.toolName();
        }

        ToolIntentDecision decision =
                aiToolIntentResolver.resolve(
                        message,
                        explicitConfirmation,
                        context
                );

        if (decision != null && decision.isConfident()) {
            return decision.toolName();
        }

        String fallbackTool =
                fallbackToolIntentResolver.resolve(
                        message,
                        explicitConfirmation,
                        context
                );

        if (fallbackTool == null || fallbackTool.isBlank()) {
            throw new IllegalArgumentException(
                    "Could not determine an approved MCP tool for this request."
            );
        }

        return fallbackTool;
    }

    private boolean isSwitchingAwayFromAwaitingTool(
            ConversationContext context,
            String intendedTool
    ) {

        if (context == null || !context.isAwaitingInput()) {
            return false;
        }

        String awaitingTool = context.awaitingTool();

        return awaitingTool != null
                && !awaitingTool.isBlank()
                && intendedTool != null
                && !intendedTool.isBlank()
                && !awaitingTool.equals(intendedTool);
    }

    private Map<String, Object> buildToolArguments(String toolName,
                                                   String message,
                                                   ConversationContext context,
                                                   String sessionKey) {

        Map<String, Object> input = new HashMap<>();

        if ("searchPolicyDocuments".equals(toolName)) {

            if (message != null && !message.isBlank()) {
                input.put("query", message.trim());
            }

        } else if ("confirmBillPay".equals(toolName)
                && context != null) {

            if (context.confirmationToken() != null
                    && !context.confirmationToken().isBlank()) {

                input.put(
                        "confirmationToken",
                        context.confirmationToken()
                );
            }

            if (context.preparedActionData() != null) {
                input.putAll(context.preparedActionData());
            }

        } else {

            boolean continuingAwaitingTool = context != null
                    && context.isAwaitingInput()
                    && toolName.equals(context.awaitingTool())
                    && context.awaitingArgumentData() != null;

            if (continuingAwaitingTool) {
                input.putAll(context.awaitingArgumentData());
            }

            Map<String, Object> extractedArguments =
                    toolArgumentExtractionService.extract(
                            toolName,
                            message,
                            context
                    );

            if (continuingAwaitingTool) {
                mergeContinuationArguments(
                        input,
                        extractedArguments,
                        context.missingFields()
                );
            } else {
                input.putAll(extractedArguments);
            }
        }

        /*
         * The agent uses the stable session key only for correlation and memory.
         * It does not resolve banking ownership or customer authorization here.
         * BankStackMCPServer receives the bearer token and enforces actor validity,
         * scopes, ownership, and banking policy at the MCP boundary.
         */
        input.putIfAbsent("actorId", sessionKey);

        return input;
    }

    private void mergeContinuationArguments(Map<String, Object> accumulatedArguments,
                                            Map<String, Object> newlyExtractedArguments,
                                            List<String> missingFields) {

        if (newlyExtractedArguments == null || newlyExtractedArguments.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : newlyExtractedArguments.entrySet()) {

            String key = entry.getKey();
            Object value = entry.getValue();

            if (key == null || key.isBlank() || isBlank(value)) {
                continue;
            }

            if (key.startsWith("_")) {
                accumulatedArguments.put(key, value);
                continue;
            }

            boolean wasMissing = missingFields != null && missingFields.contains(key);
            boolean existingBlank = isBlank(accumulatedArguments.get(key));

            if (wasMissing || existingBlank) {
                accumulatedArguments.put(key, value);
            }
        }
    }

    private boolean isBlank(Object value) {
        return value == null || value.toString().trim().isEmpty();
    }

    private Map<String, Object> removeInternalArguments(
            Map<String, Object> toolArguments
    ) {

        Map<String, Object> cleaned =
                new HashMap<>(
                        toolArguments == null
                                ? Map.of()
                                : toolArguments
                );

        cleaned.keySet().removeIf(
                key -> key != null && key.startsWith("_")
        );

        return cleaned;
    }

    private void updateConversationContextAfterToolExecution(
            String sessionKey,
            boolean explicitConfirmation,
            String intendedTool,
            ToolExecutionResult result,
            String safeAnswer
    ) {

        if (result.isRequiresConfirmation()
                || result.prepared()) {

            String token = result.getConfirmationToken();

            if (token == null || token.isBlank()) {
                return;
            }

            contextStore.savePreparedAction(
                    sessionKey,
                    resolvePreparedActionType(
                            result.getActionType(),
                            intendedTool
                    ),
                    policyService.summarizePreparedAction(safeAnswer),
                    token,
                    result.getData()
            );

            return;
        }

        if (explicitConfirmation || result.executed()) {
            contextStore.clear(sessionKey);
        }
    }

    private PreparedActionType resolvePreparedActionType(
            String actionType,
            String toolName
    ) {

        if ("BILL_PAY".equalsIgnoreCase(actionType)
                || "prepareBillPay".equals(toolName)
                || "confirmBillPay".equals(toolName)) {

            return PreparedActionType.BILL_PAY;
        }

        return PreparedActionType.NONE;
    }

    private boolean isExplicitRejection(String message) {

        if (message == null || message.isBlank()) {
            return false;
        }

        String text = message.trim().toLowerCase();

        return text.equals("no")
                || text.equals("nope")
                || text.equals("nah")
                || text.equals("nahi")
                || text.equals("no thanks")
                || text.equals("don't use it")
                || text.equals("do not use it");
    }

    private void rememberSuccessfulToolAction(String actorId,
                                              String intendedTool,
                                              Map<String, Object> toolArguments,
                                              ToolExecutionResult result) {

        if (actorId == null
                || actorId.isBlank()) {
            return;
        }

        if (!"prepareBillPay".equals(intendedTool)) {
            return;
        }

        if (result == null
                || result.failed()
                || result.needsInput()) {
            return;
        }

        Map<String, Object> safeMemory = new HashMap<>();

        putIfPresent(
                safeMemory,
                "debtorAccountId",
                toolArguments.get("debtorAccountId")
        );

        putIfPresent(
                safeMemory,
                "billerReferenceNumber",
                toolArguments.get("billerReferenceNumber")
        );

        putIfPresent(
                safeMemory,
                "currency",
                toolArguments.get("currency")
        );

        if (safeMemory.isEmpty()) {
            return;
        }

        actionMemoryService.rememberSuccessfulAction(
                actorId,
                intendedTool,
                "bill payment",
                safeMemory
        );
    }

    private void putIfPresent(Map<String, Object> target,
                              String key,
                              Object value) {

        if (value != null && !value.toString().isBlank()) {
            target.put(key, value);
        }
    }

    private List<String> remainingMissingFields(List<String> originalMissingFields,
                                                Map<String, Object> recoveredArguments) {

        if (originalMissingFields == null || originalMissingFields.isEmpty()) {
            return List.of();
        }

        if (recoveredArguments == null || recoveredArguments.isEmpty()) {
            return originalMissingFields;
        }

        return originalMissingFields.stream()
                .filter(field -> !recoveredArguments.containsKey(field))
                .toList();
    }

    private Map<String, Object> recoveredSuggestionForResponse(Map<String, Object> recoveredArguments) {

        Map<String, Object> suggestion = new LinkedHashMap<>();

        if (recoveredArguments == null || recoveredArguments.isEmpty()) {
            return suggestion;
        }

        Object account = recoveredArguments.get("debtorAccountId");
        if (!isBlank(account)) {
            suggestion.put("debtorAccountIdMasked", maskAccount(account));
        }

        Object billerReference = recoveredArguments.get("billerReferenceNumber");
        if (!isBlank(billerReference)) {
            suggestion.put("billerReferenceNumber", billerReference);
        }

        Object currency = recoveredArguments.get("currency");
        if (!isBlank(currency)) {
            suggestion.put("currency", currency);
        }

        return suggestion;
    }

    private Map<String, Object> sanitizeArgumentsForResponse(Map<String, Object> arguments) {

        Map<String, Object> sanitized = removeInternalArguments(arguments);

        if (sanitized.isEmpty()) {
            return Map.of();
        }

        return sanitized;
    }

    private String maskAccount(Object value) {

        String text = value == null ? null : value.toString();

        if (text == null || text.length() < 4) {
            return "****";
        }

        return "****" + text.substring(text.length() - 4);
    }

    private String actionTypeForTool(String toolName) {

        if ("prepareBillPay".equals(toolName) || "confirmBillPay".equals(toolName)) {
            return "BILL_PAY";
        }

        if ("getAccountBalance".equals(toolName)) {
            return "READ_BALANCE";
        }

        if ("getTransactions".equals(toolName)) {
            return "READ_TRANSACTIONS";
        }

        if ("getCustomerProfile".equals(toolName)) {
            return "READ_CUSTOMER_PROFILE";
        }

        if ("getPaymentStatus".equals(toolName)) {
            return "READ_PAYMENT_STATUS";
        }

        return toolName;
    }

    private record ToolRouteOutcome(
            String answer,
            String toolName,
            Map<String, Object> toolArguments,
            ToolExecutionResult toolResult
    ) {
    }

}
