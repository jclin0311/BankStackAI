package com.bankstack.rag.service;

import com.bankstack.rag.api.RagAskResponse;
import com.bankstack.rag.assemble.AssembledContext;
import com.bankstack.rag.assemble.ContextAssemblyOptions;
import com.bankstack.rag.assemble.ContextAssemblyService;
import com.bankstack.rag.assemble.ContextSnippet;
import com.bankstack.rag.memory.ConversationMemoryService;
import com.bankstack.rag.prompt.CtidrPrompt;
import com.bankstack.rag.prompt.CtidrPromptBuilder;
import com.bankstack.rag.prompt.PromptTask;
import com.bankstack.rag.prompt.TaskClassifier;
import com.bankstack.rag.prompt.TaskType;
import com.bankstack.rag.retrieve.HybridSearchResult;
import com.bankstack.rag.retrieve.HybridSearchService;
import com.bankstack.rag.security.AccessContext;
import com.bankstack.rag.verify.CitationVerifierService;
import com.bankstack.rag.verify.VerifiedAnswer;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class RagAnswerService {

    private static final String EMPTY_CONTEXT_FALLBACK =
            "No relevant policy found in available documents.";

    private final TaskClassifier taskClassifier;
    private final HybridSearchService hybridSearchService;
    private final ContextAssemblyService contextAssemblyService;
    private final CtidrPromptBuilder ctidrPromptBuilder;
    private final CitationVerifierService citationVerifierService;
    private final ConversationMemoryService conversationMemoryService;
    private final ChatClient chatClient;

    private final double minTopScore;

    public RagAnswerService(
            TaskClassifier taskClassifier,
            HybridSearchService hybridSearchService,
            ContextAssemblyService contextAssemblyService,
            CtidrPromptBuilder ctidrPromptBuilder,
            CitationVerifierService citationVerifierService,
            ConversationMemoryService conversationMemoryService,
            ChatClient.Builder chatClientBuilder,
            @Value("${bankstack.rag.relevance.min-top-score:0.45}") double minTopScore
     //       @Value("${bankstack.rag.relevance.min-context-similarity:0.50}") double minContextSimilarity
    ) {
        this.taskClassifier = taskClassifier;
        this.hybridSearchService = hybridSearchService;
        this.contextAssemblyService = contextAssemblyService;
        this.ctidrPromptBuilder = ctidrPromptBuilder;
        this.citationVerifierService = citationVerifierService;
        this.conversationMemoryService = conversationMemoryService;
        this.chatClient = chatClientBuilder.build();
        this.minTopScore = minTopScore;
    }

    public RagAskResponse answer(String query, AccessContext accessContext) {
        return answer(query, accessContext, null);
    }

    public RagAskResponse answer(String query, AccessContext accessContext, String conversationId) {
        
    	TaskType taskType = taskClassifier.classify(query);
        PromptTask task = new PromptTask(taskType, query);
        
        
        if (taskType == TaskType.REFUSAL) {
            CtidrPrompt prompt = ctidrPromptBuilder.build(task, null);
            String memoryBlock = conversationMemoryService.renderForPrompt(conversationId);
            String rawAnswer = generateAnswer(prompt, memoryBlock);

            String effectiveConversationId =
                    conversationMemoryService.resolveConversationId(conversationId);

            return new RagAskResponse(
                    rawAnswer,
                    taskType,
                    false,
                    List.of(),
                    "",
                    effectiveConversationId
            );
        }
        

        List<HybridSearchResult> hybridResults = hybridSearchService.search(query, 8, accessContext);

        /*
         * First relevance gate:
         * If retrieval score is weak, do not continue.
         */
        if (!hasRelevantEvidence(hybridResults)) {
            return emptyResponse(taskType, "");
        }
        
    
        AssembledContext assembledContext = contextAssemblyService.assemble(
                hybridResults,
                ContextAssemblyOptions.defaults()
        );

        if (assembledContext.snippets().isEmpty()) {
            return emptyResponse(taskType, assembledContext.renderedContext());
        }

       
        
        CtidrPrompt prompt = ctidrPromptBuilder.build(task, assembledContext);
        String memoryBlock = conversationMemoryService.renderForPrompt(conversationId);
        String rawAnswer = generateAnswer(prompt, memoryBlock);

      
        VerifiedAnswer verifiedAnswer = citationVerifierService.verify(rawAnswer, assembledContext);

       
        
        String effectiveConversationId =
                conversationMemoryService.resolveConversationId(conversationId);

        
        
        if (verifiedAnswer.fullyVerified()) {
        	
            conversationMemoryService.remember(effectiveConversationId, query, verifiedAnswer.answer());
        }

        return new RagAskResponse(
                verifiedAnswer.answer(),
                taskType,
                verifiedAnswer.fullyVerified(),
                verifiedAnswer.checks(),
                assembledContext.renderedContext(),
                effectiveConversationId
        );
    }

    private RagAskResponse emptyResponse(TaskType taskType, String renderedContext) {
        return new RagAskResponse(
                EMPTY_CONTEXT_FALLBACK,
                taskType,
                false,
                List.of(),
                renderedContext == null ? "" : renderedContext,
                ""
        );
    }

    private boolean hasRelevantEvidence(List<HybridSearchResult> results) {
        return results != null
                && !results.isEmpty()
                && results.get(0).finalScore() >= minTopScore;
    }

    private String generateAnswer(CtidrPrompt prompt, String memoryBlock) {
        String optionalMemory = memoryBlock == null || memoryBlock.isBlank()
                ? ""
                : memoryBlock + "\n\n";

        String content = chatClient.prompt()
                .system(prompt.systemPrompt())
                .user("""
                        %s%s

                        %s
                        """.formatted(optionalMemory, prompt.contextBlock(), prompt.userPrompt()))
                .call()
                .content();

        return content == null ? "" : content.trim();
    }
    
    
    private boolean hasLexicalSupport(List<HybridSearchResult> results) {
        if (results == null || results.isEmpty()) {
            return false;
        }

        HybridSearchResult top = results.get(0);

        return top.keywordScore() > 0.0 || top.finalScore() >= 0.70;
    }

}
