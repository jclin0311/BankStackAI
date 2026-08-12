package com.bankstack.mcp.client;

import com.bankstack.mcp.dto.PolicySearchResponse;
import com.bankstack.mcp.dto.RagAskRequest;
import com.commons.security.FeignTokenRelayConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "rag-policy-search-client",
        url = "${downstream.services.rag.base-url}",
        configuration = FeignTokenRelayConfig.class
)
public interface PolicySearchClient {

    @PostMapping("/api/rag/ask")
    PolicySearchResponse ask(@RequestBody RagAskRequest request);

    default PolicySearchResponse searchPolicyDocuments(String query) {
        return ask(new RagAskRequest(query, null));
    }
}
