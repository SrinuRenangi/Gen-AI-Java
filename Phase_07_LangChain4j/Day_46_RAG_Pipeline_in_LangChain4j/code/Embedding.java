package com.genai.langchain4j.rag;

/**
 * Encapsulates a dense numerical vector representation of semantic text.
 * Matches dev.langchain4j.data.embedding.Embedding.
 */
public record Embedding(float[] vector) {

    public double cosineSimilarity(Embedding other) {
        if (vector.length != other.vector.length) {
            throw new IllegalArgumentException("Vector dimension mismatch: " + vector.length + " vs " + other.vector.length);
        }

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vector.length; i++) {
            dot += vector[i] * other.vector[i];
            normA += vector[i] * vector[i];
            normB += other.vector[i] * other.vector[i];
        }

        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
