package com.genai.enterprise.vectordb;

/**
 * Optimized mathematical distance functions matching PostgreSQL pgvector operators.
 */
public class DistanceMetric {

    /**
     * Cosine distance: 1 - cosine_similarity (Matching pgvector '<=>' operator).
     */
    public static double cosineDistance(float[] v1, float[] v2) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            normA += v1[i] * v1[i];
            normB += v2[i] * v2[i];
        }
        if (normA == 0.0 || normB == 0.0) return 1.0;
        double similarity = dot / (Math.sqrt(normA) * Math.sqrt(normB));
        return Math.max(0.0, 1.0 - similarity);
    }

    /**
     * Euclidean L2 distance: sqrt(sum((a - b)^2)) (Matching pgvector '<->' operator).
     */
    public static double euclideanDistance(float[] v1, float[] v2) {
        double sum = 0.0;
        for (int i = 0; i < v1.length; i++) {
            double diff = v1[i] - v2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
}
