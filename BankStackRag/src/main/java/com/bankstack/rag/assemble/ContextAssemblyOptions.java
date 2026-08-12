package com.bankstack.rag.assemble;

public record ContextAssemblyOptions(
        int maxSnippets,
        int maxEstimatedTokens,
        int maxCharactersPerSnippet
) {

    public static ContextAssemblyOptions defaults() {
        return new ContextAssemblyOptions(
        		 3,
                 1800,
                 2500
        );
    }
}
