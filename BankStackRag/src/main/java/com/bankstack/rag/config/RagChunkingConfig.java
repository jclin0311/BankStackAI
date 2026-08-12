package com.bankstack.rag.config;

import com.bankstack.rag.chunking.MarkdownStructuralChunker;
import com.bankstack.rag.chunking.StructuralChunker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the current structural chunking strategy.
 *
 * Today:
 * - simple markdown-style heading chunker
 *
 * Later:
 * - policy-aware advanced chunker
 * - PDF-aware chunker
 * - HTML/Confluence chunker
 */
@Configuration
public class RagChunkingConfig {

    @Bean
    public StructuralChunker structuralChunker() {
        return new MarkdownStructuralChunker();
    }
}