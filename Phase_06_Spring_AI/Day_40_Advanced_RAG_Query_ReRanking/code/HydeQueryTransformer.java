package com.genai.springai.advancedrag;

import java.util.List;

/**
 * Implements Hypothetical Document Embeddings (HyDE).
 * Generates a hypothetical passage that would answer the user's question,
 * and uses that dense passage for vector similarity retrieval rather than the sparse query!
 */
public class HydeQueryTransformer implements QueryTransformer {

    @Override
    public String transform(String rawUserQuery) {
        // In production, this prompts a fast lightweight LLM:
        // "Write a passage that answers this question: " + rawUserQuery
        return "Hypothetical passage answering '" + rawUserQuery + "': "
                + "In enterprise distributed architectures, gateway timeout 504 errors typically occur "
                + "when downstream microservices take longer than the reverse proxy timeout threshold. "
                + "Resolution requires tuning proxy connection timeouts and optimizing downstream database queries.";
    }

    public List<String> expandQueries(String originalQuery) {
        return List.of(
                originalQuery,
                "Root causes and troubleshooting steps for " + originalQuery,
                "Configuring timeouts and architectural fixes for " + originalQuery
        );
    }
}
