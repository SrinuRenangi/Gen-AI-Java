package com.genai.langchain4j.rag;

import java.util.List;

/**
 * Enterprise Retrieval Augmentor.
 * Formulates the augmented prompt by querying ContentRetriever and injecting context blocks.
 * Matches dev.langchain4j.rag.DefaultRetrievalAugmentor.
 */
public class RetrievalAugmentor {

    private final ContentRetriever retriever;

    public RetrievalAugmentor(ContentRetriever retriever) {
        this.retriever = retriever;
    }

    public record AugmentedQuery(String originalQuery, String augmentedPrompt, List<TextSegment> citedSources) {}

    public AugmentedQuery augment(String userQuery) {
        List<TextSegment> relevantSegments = retriever.retrieve(userQuery);

        StringBuilder sb = new StringBuilder();
        sb.append("Answer the customer inquiry based exclusively on the following enterprise documentation:\n\n");
        sb.append("--- BEGIN DOCUMENTATION CONTEXT ---\n");

        for (int i = 0; i < relevantSegments.size(); i++) {
            TextSegment seg = relevantSegments.get(i);
            sb.append(String.format("[Source %d] (doc: %s, section: %s):\n%s\n\n",
                i + 1,
                seg.metadata().getOrDefault("document", "Unknown"),
                seg.metadata().getOrDefault("section", "General"),
                seg.text()
            ));
        }

        sb.append("--- END DOCUMENTATION CONTEXT ---\n\n");
        sb.append("User Inquiry: ").append(userQuery).append("\n");
        sb.append("If the documentation does not contain the answer, state clearly that you do not know.");

        return new AugmentedQuery(userQuery, sb.toString(), relevantSegments);
    }
}
