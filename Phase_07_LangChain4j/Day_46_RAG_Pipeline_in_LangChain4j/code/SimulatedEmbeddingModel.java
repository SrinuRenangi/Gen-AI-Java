package com.genai.langchain4j.rag;

import java.util.List;

/**
 * Deterministic 8-dimensional embedding model mapping semantic domains to vector space.
 */
public class SimulatedEmbeddingModel implements EmbeddingModel {

    @Override
    public Embedding embed(String text) {
        float[] v = new float[8];
        String lower = text.toLowerCase();

        // 0: Containers / Orchestration
        if (lower.contains("kubernetes") || lower.contains("docker") || lower.contains("container") || lower.contains("pod")) v[0] += 1.0f;
        // 1: Java / Spring / Language
        if (lower.contains("java") || lower.contains("spring") || lower.contains("boot") || lower.contains("jvm")) v[1] += 1.0f;
        // 2: Database / Storage
        if (lower.contains("database") || lower.contains("sql") || lower.contains("postgres") || lower.contains("pgvector") || lower.contains("storage")) v[2] += 1.0f;
        // 3: Security / Auth
        if (lower.contains("security") || lower.contains("auth") || lower.contains("jwt") || lower.contains("oauth") || lower.contains("rbac")) v[3] += 1.0f;
        // 4: Messaging / Streaming
        if (lower.contains("kafka") || lower.contains("event") || lower.contains("stream") || lower.contains("messaging")) v[4] += 1.0f;
        // 5: Cloud / Infrastructure
        if (lower.contains("cloud") || lower.contains("aws") || lower.contains("cluster") || lower.contains("network")) v[5] += 1.0f;
        // 6: Performance / Latency
        if (lower.contains("performance") || lower.contains("latency") || lower.contains("throughput") || lower.contains("p99")) v[6] += 1.0f;
        // 7: Compliance / Policy
        if (lower.contains("policy") || lower.contains("compliance") || lower.contains("refund") || lower.contains("audit") || lower.contains("legal")) v[7] += 1.0f;

        // Normalize vector to unit length
        float norm = 0.0f;
        for (float val : v) norm += val * val;
        if (norm > 0.0f) {
            float length = (float) Math.sqrt(norm);
            for (int i = 0; i < v.length; i++) v[i] /= length;
        } else {
            // General neutral baseline
            for (int i = 0; i < v.length; i++) v[i] = 1.0f / (float) Math.sqrt(8);
        }

        return new Embedding(v);
    }

    @Override
    public List<Embedding> embedAll(List<TextSegment> textSegments) {
        return textSegments.stream().map(s -> embed(s.text())).toList();
    }
}
