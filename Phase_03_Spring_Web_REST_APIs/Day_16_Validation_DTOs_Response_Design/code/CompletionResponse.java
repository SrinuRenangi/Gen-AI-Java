package code;

import java.time.Instant;
import java.util.Map;

/**
 * Modern Java 21 Record DTO for AI Completion Responses.
 *
 * Demonstrates:
 * 1. Clean response modeling without leaking internal LLM driver state.
 * 2. Standardized token usage accounting.
 * 3. Enterprise audit metadata (timestamp, latency, finishReason).
 */
public record CompletionResponse(
    String id,
    String model,
    String completion,
    TokenUsage usage,
    long latencyMs,
    String finishReason,
    Instant timestamp
) {
    /**
     * Nested DTO record for token accounting.
     */
    public record TokenUsage(
        int promptTokens,
        int completionTokens,
        int totalTokens
    ) {
        public TokenUsage {
            if (promptTokens < 0 || completionTokens < 0) {
                throw new IllegalArgumentException("Token counts cannot be negative");
            }
        }

        public static TokenUsage calculate(String prompt, String completion) {
            // Approximation: ~4 chars per token for English text
            int pTokens = Math.max(1, prompt.length() / 4);
            int cTokens = Math.max(1, completion.length() / 4);
            return new TokenUsage(pTokens, cTokens, pTokens + cTokens);
        }
    }

    /**
     * Factory helper to create a successful completion response.
     */
    public static CompletionResponse success(
        String id,
        String model,
        String prompt,
        String completion,
        long latencyMs
    ) {
        TokenUsage usage = TokenUsage.calculate(prompt, completion);
        return new CompletionResponse(
            id,
            model,
            completion,
            usage,
            latencyMs,
            "stop",
            Instant.now()
        );
    }
}
