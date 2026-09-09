package com.genai.langchain4j.rag;

import java.util.List;

/**
 * Implementation of ContentRetriever backed by an EmbeddingStore and EmbeddingModel.
 * Matches dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever.
 */
public class EmbeddingStoreContentRetriever implements ContentRetriever {

    private final InMemoryEmbeddingStore store;
    private final EmbeddingModel embeddingModel;
    private final int maxResults;
    private final double minScore;

    public EmbeddingStoreContentRetriever(InMemoryEmbeddingStore store, EmbeddingModel embeddingModel, int maxResults, double minScore) {
        this.store = store;
        this.embeddingModel = embeddingModel;
        this.maxResults = maxResults;
        this.minScore = minScore;
    }

    @Override
    public List<TextSegment> retrieve(String query) {
        Embedding queryEmbedding = embeddingModel.embed(query);
        List<EmbeddingMatch<TextSegment>> matches = store.findRelevant(queryEmbedding, maxResults, minScore);

        return matches.stream().map(EmbeddingMatch::embedded).toList();
    }
}
