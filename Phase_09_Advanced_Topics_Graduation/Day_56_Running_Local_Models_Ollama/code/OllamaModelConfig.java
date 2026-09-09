package com.genai.enterprise.localmodel;

/**
 * Configuration for local open-weight model inference (Ollama / LocalAI).
 */
public record OllamaModelConfig(
        String baseUrl,
        String modelName,
        double temperature,
        int contextWindowTokens,
        boolean stream
) {
    public static OllamaModelConfig defaultLlama32() {
        return new OllamaModelConfig("http://localhost:11434", "llama3.2", 0.7, 4096, true);
    }

    public static OllamaModelConfig lowMemory1B() {
        return new OllamaModelConfig("http://localhost:11434", "llama3.2:1b", 0.2, 2048, false);
    }
}
