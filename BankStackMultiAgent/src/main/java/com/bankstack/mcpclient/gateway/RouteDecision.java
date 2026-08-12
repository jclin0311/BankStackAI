package com.bankstack.mcpclient.gateway;

public record RouteDecision(
        Route route,
        double confidence,
        String reason
) {
    public boolean isConfident() {
        return route != null && confidence >= 0.75;
    }

    public static RouteDecision fallback() {
        return new RouteDecision(null, 0.0, "fallback_required");
    }
}