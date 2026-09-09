package com.genai.langchain4j.rag;

import java.util.*;

/**
 * Thread-safe vector store providing cosine similarity searches.
 * Matches dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore.
 */
public class InMemoryEmbeddingStore {

    private final List<Entry> entries = new ArrayList<>();

    public record Entry(String id, Embedding embedding, TextSegment segment) {}

    public synchronized void add(String id, Embedding embedding, TextSegment segment) {
        entries.add(new Entry(id, embedding, segment));
    }

    public synchronized void addAll(List<String> ids, List<Embedding> embeddings, List<TextSegment> segments) {
        for (int i = 0; i < ids.size(); i++) {
            add(ids.get(i), embeddings.get(i), segments.get(i));
        }
    }

    public synchronized List<EmbeddingMatch<TextSegment>> findRelevant(Embedding referenceEmbedding, int maxResults, double minScore) {
        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        for (Entry e : entries) {
            double score = e.embedding().cosineSimilarity(referenceEmbedding);
            if (score >= minScore) {
                matches.add(new EmbeddingMatch<>(score, e.id(), e.embedding(), e.segment()));
            }
        }
        Collections.sort(matches);
        return matches.subList(0, Math.min(matches.size(), maxResults));
    }

    public synchronized int size() {
        return entries.size();
    }
}
