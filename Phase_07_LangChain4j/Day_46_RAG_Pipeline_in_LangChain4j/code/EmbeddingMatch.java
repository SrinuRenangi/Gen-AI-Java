package com.genai.langchain4j.rag;

/**
 * Encapsulates a search match result containing score, ID, and embedded payload.
 * Matches dev.langchain4j.store.embedding.EmbeddingMatch.
 */
public record EmbeddingMatch<T>(
    double score,
    String embeddingId,
    Embedding embedding,
    T embedded
) implements Comparable<EmbeddingMatch<T>> {
    @Override
    public int compareTo(EmbeddingMatch<T> o) {
        return Double.compare(o.score, this.score); // descending order
    }
}
