package com.bankstack.rag.config;

import com.bankstack.rag.retrieve.VectorSearchService;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RagVectorConfig wires our retrieval service to Spring AI's VectorStore.
 *
 * Spring Boot autoconfiguration creates the concrete PgVector-backed VectorStore
 * when the pgvector starter and datasource are configured correctly.
 */
@Configuration
public class RagVectorConfig {

    @Bean
    public VectorSearchService vectorSearchService(VectorStore vectorStore) {
        return new VectorSearchService(vectorStore);
    }
}