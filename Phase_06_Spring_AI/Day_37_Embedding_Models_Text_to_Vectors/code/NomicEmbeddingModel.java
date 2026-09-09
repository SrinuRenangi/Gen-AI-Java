package com.genai.springai.embeddings;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Simulates Ollama's nomic-embed-text model (768 dimensions).
 * Generates deterministic, dense semantic vector embeddings.
 */
public class NomicEmbeddingModel implements EmbeddingModel {

    private static final int DIMENSIONS = 768;

    @Override
    public float[] embed(String text) {
        float[] vector = new float[DIMENSIONS];
        String lower = text.toLowerCase();

        // 1. Semantic cluster biasing
        boolean isJava = lower.contains("java") || lower.contains("thread") || lower.contains("jvm") || lower.contains("spring");
        boolean isDatabase = lower.contains("sql") || lower.contains("database") || lower.contains("postgres") || lower.contains("vector");
        boolean isCooking = lower.contains("bake") || lower.contains("cookie") || lower.contains("recipe") || lower.contains("food");
        boolean isPolicy = lower.contains("sla") || lower.contains("uptime") || lower.contains("benefit") || lower.contains("enrollment") || lower.contains("policy") || lower.contains("response time");

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));

            for (int i = 0; i < DIMENSIONS; i++) {
                byte b = hash[i % hash.length];
                float baseVal = (float) ((b & 0xFF) - 128) / 256.0f;

                if (isJava && i < 250) {
                    baseVal += 0.8f;
                } else if (isDatabase && i >= 250 && i < 500) {
                    baseVal += 0.8f;
                } else if (isCooking && i >= 500) {
                    baseVal += 0.8f;
                } else if (isPolicy && i >= 100 && i < 350) {
                    baseVal += 0.8f;
                }
                vector[i] = baseVal;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Return L2 normalized vector
        return VectorMath.normalize(vector);
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        List<float[]> results = new ArrayList<>(texts.size());
        for (String t : texts) {
            results.add(embed(t));
        }
        return results;
    }

    @Override
    public EmbeddingResponse call(List<String> texts) {
        List<Embedding> embeddings = new ArrayList<>();
        long totalTokens = 0;

        for (int i = 0; i < texts.size(); i++) {
            String t = texts.get(i);
            embeddings.add(new Embedding(embed(t), i));
            totalTokens += t.length() / 4 + 2;
        }

        return new EmbeddingResponse(embeddings, new Usage(totalTokens, totalTokens));
    }

    @Override
    public int dimensions() {
        return DIMENSIONS;
    }

    @Override
    public String getModelName() {
        return "nomic-embed-text (Ollama 768-dim)";
    }
}
