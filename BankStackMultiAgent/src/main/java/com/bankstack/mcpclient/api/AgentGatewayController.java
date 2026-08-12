package com.bankstack.mcpclient.api;

import com.bankstack.mcpclient.gateway.AgentGatewayService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentGatewayController {

    private final AgentGatewayService agentGatewayService;

    public AgentGatewayController(AgentGatewayService agentGatewayService) {
        this.agentGatewayService = agentGatewayService;
    }

    @PostMapping("/chat")
    public GatewayChatResponse chat(@Valid @RequestBody GatewayChatRequest request) {
        return agentGatewayService.handle(request);
    }
}