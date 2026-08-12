package com.bankstack.mcpclient.api;

import com.bankstack.mcpclient.multiagent.orchestration.MultiAgentExecutionResponse;
import com.bankstack.mcpclient.multiagent.orchestration.MultiAgentOrchestratorService;
import com.bankstack.mcpclient.multiagent.router.MultiAgentRouter;
import com.bankstack.mcpclient.multiagent.router.RoutingPlan;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/agent")
public class MultiAgentRoutingController {

    private final MultiAgentOrchestratorService orchestratorService;

    public MultiAgentRoutingController(MultiAgentRouter multiAgentRouter,
                                       MultiAgentOrchestratorService orchestratorService) {
        this.orchestratorService = orchestratorService;
    }

 
    @PostMapping("/multi-agent-chat")
    public MultiAgentExecutionResponse multiAgentChat(@Valid @RequestBody GatewayChatRequest request) {
        return orchestratorService.execute(request.message());
    }
}
