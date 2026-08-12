package com.bankstack.mcpclient.multiagent.router;

import java.util.List;

public record RoutingPlan(
        boolean multiDomain,
        int taskCount,
        String summary,
        List<RoutedTask> tasks
) {
    public static RoutingPlan of(List<RoutedTask> tasks) {
        List<RoutedTask> safeTasks = tasks == null ? List.of() : List.copyOf(tasks);
        boolean multiDomain = safeTasks.stream()
                .map(RoutedTask::agentType)
                .distinct()
                .count() > 1;

        String summary = multiDomain
                ? "Multi-domain request detected. Router must delegate to specialized domain agents."
                : "Single-domain request detected. Router can delegate to one domain agent.";

        return new RoutingPlan(
                multiDomain,
                safeTasks.size(),
                summary,
                safeTasks
        );
    }
}
