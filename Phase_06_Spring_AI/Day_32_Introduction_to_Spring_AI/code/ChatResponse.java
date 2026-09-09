package com.genai.springai.core;

import java.util.List;

/**
 * Simulates Spring AI's ChatResponse encapsulating Generations and token Usage metadata.
 */
public record ChatResponse(
        List<Generation> generations,
        UsageMetadata usage
) {
    public record Generation(
            Message.AssistantMessage output,
            String finishReason
    ) {}

    public record UsageMetadata(
            long promptTokens,
            long generationTokens,
            long totalTokens
    ) {}

    public Generation getResult() {
        if (generations == null || generations.isEmpty()) {
            throw new IllegalStateException("No generation results present in response");
        }
        return generations.get(0);
    }
}
