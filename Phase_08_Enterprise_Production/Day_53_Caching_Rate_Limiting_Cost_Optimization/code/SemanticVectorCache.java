package com.genai.enterprise.costopt;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Semantic vector cache matching queries based on vector cosine similarity threshold.
 */
public class SemanticVectorCache {

    public record CachedItem(String query, float[] embedding, String response, long timestampMs) {}

    private final List<CachedItem> cacheStore = new CopyOnWriteArrayList<>();
    private final double similarityThreshold;
    private final AtomicLong semanticHits = new AtomicLong(0);
    private final AtomicLong semanticMisses = new AtomicLong(0);

    public SemanticVectorCache(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public Optional<CachedItem> findSimilar(float[] queryVector) {
        CachedItem bestMatch = null;
        double highestSimilarity = -1.0;

        for (CachedItem item : cacheStore) {
            double similarity = cosineSimilarity(queryVector, item.embedding());
            if (similarity > highestSimilarity) {
                highestSimilarity = similarity;
                bestMatch = item;
            }
        }

        if (bestMatch != null && highestSimilarity >= similarityThreshold) {
            semanticHits.incrementAndGet();
            return Optional.of(bestMatch);
        }

        semanticMisses.incrementAndGet();
        return Optional.empty();
    }

    public void put(String query, float[] embedding, String response) {
        cacheStore.add(new CachedItem(query, embedding, response, System.currentTimeMillis()));
    }

    public long getSemanticHits() { return semanticHits.get(); }
    public long getSemanticMisses() { return semanticMisses.get(); }

    public static double cosineSimilarity(float[] v1, float[] v2) {
        if (v1.length != v2.length) {
            throw new IllegalArgumentException("Vector dimension mismatch: " + v1.length + " vs " + v2.length);
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dotProduct += v1[i] * v2[i];
            normA += v1[i] * v1[i];
            normB += v2[i] * v2[i];
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
