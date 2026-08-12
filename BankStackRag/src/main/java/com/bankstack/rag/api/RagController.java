package com.bankstack.rag.api;

import com.bankstack.rag.security.AccessContext;
import com.bankstack.rag.security.RagAccessContextMapper;
import com.bankstack.rag.service.RagAnswerService;
import com.commons.security.context.AuthenticatedCaller;
import com.commons.security.context.AuthenticatedCallerProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/api/rag")
public class RagController {

    private final RagAnswerService ragAnswerService;
    private final AuthenticatedCallerProvider callerProvider;
    private final RagAccessContextMapper accessContextMapper;

    public RagController(RagAnswerService ragAnswerService,
                         AuthenticatedCallerProvider callerProvider,
                         RagAccessContextMapper accessContextMapper) {
        this.ragAnswerService = ragAnswerService;
        this.callerProvider = callerProvider;
        this.accessContextMapper = accessContextMapper;
    }

    @PostMapping("/ask")
    public RagAskResponse ask(@RequestBody RagAskRequest request) {
        AuthenticatedCaller caller = callerProvider.currentCaller();
        AccessContext accessContext = accessContextMapper.from(caller);

        log.info("RAG /ask invoked subject={} query={}", caller.subject(), request.query());

        return ragAnswerService.answer(
                request.query(),
                accessContext,
                request.conversationId()
        );
    }
}
