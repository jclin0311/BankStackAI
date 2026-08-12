package com.bankstack.mcp.api;

import com.bankstack.mcp.service.ToolInvocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/mcp")
public class ToolTestController {

    private final ToolInvocationService toolInvocationService;

    public ToolTestController(ToolInvocationService toolInvocationService) {
        this.toolInvocationService = toolInvocationService;
    }

    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> listTools() {
        return ResponseEntity.ok(toolInvocationService.listTools());
    }

    @PostMapping("/invoke")
    public ResponseEntity<Object> invoke(@RequestBody ToolInvokeRequest request) {
        Object response = toolInvocationService.invoke(request);
        return ResponseEntity.ok(response);
    }
}