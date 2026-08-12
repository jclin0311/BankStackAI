package com.bankstack.mcpclient.gateway;

import org.springframework.stereotype.Component;

@Component
public class RoutePolicyService {

    private final SafetyRouteGuard safetyRouteGuard;
    private final DeterministicIntentRouter deterministicIntentRouter;
    private final AiRouteClassifier aiRouteClassifier;
    private final FallbackRouteClassifier fallbackRouteClassifier;

    public RoutePolicyService(SafetyRouteGuard safetyRouteGuard,
                              DeterministicIntentRouter deterministicIntentRouter,
                              AiRouteClassifier aiRouteClassifier,
                              FallbackRouteClassifier fallbackRouteClassifier) {
        this.safetyRouteGuard = safetyRouteGuard;
        this.deterministicIntentRouter = deterministicIntentRouter;
        this.aiRouteClassifier = aiRouteClassifier;
        this.fallbackRouteClassifier = fallbackRouteClassifier;
    }

    public Route classify(String message,
                          boolean explicitConfirmation,
                          ConversationContext context) {

        if (safetyRouteGuard.isUnsafe(message)) {
            return Route.REFUSE;
        }

        AgentDecision deterministicDecision =
                deterministicIntentRouter.route(message, explicitConfirmation, context);

        Route deterministicRoute = toRoute(deterministicDecision);

        if (deterministicRoute != null) {
            return deterministicRoute;
        }

        RouteDecision decision = aiRouteClassifier.classify(message, context);

        if (decision != null && decision.isConfident()) {
            return decision.route();
        }

        return fallbackRouteClassifier.classify(message);
    }

    private Route toRoute(AgentDecision decision) {
        if (decision == null || decision.route() == null || decision.route().isBlank()) {
            return null;
        }

        try {
            return Route.valueOf(decision.route().trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}