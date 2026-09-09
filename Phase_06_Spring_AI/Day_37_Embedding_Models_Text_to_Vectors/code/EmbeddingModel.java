package com.genai.springai.embeddings;

import java.util.List;

/**
 * Simulates Spring AI's EmbeddingModel interface.
 */
public interface EmbeddingModel {

    record Embedding(float[] output, int index) {}

    record Usage(long promptTokens, long totalTokens) {}

    record EmbeddingResponse(List<Embedding> results, Usage usage) {}

    float[] embed(String text);

    List<float[]> embed(List<String> texts);

    EmbeddingResponse call(List<String> texts);

    int dimensions();

    String getModelName();
}
