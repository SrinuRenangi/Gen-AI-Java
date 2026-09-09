package code;

import java.util.Map;

/**
 * Thrown when prompt + completion tokens exceed the maximum context window of the model.
 *
 * Mapped to: HTTP 400 Bad Request.
 * Non-retryable: Sending the identical prompt again will fail identically.
 */
public class ContextWindowExceededException extends GenAIException {

    private final int actualTokens;
    private final int maxAllowedTokens;

    public ContextWindowExceededException(String modelName, int actualTokens, int maxAllowedTokens) {
        super(
            "Prompt token length (" + actualTokens + ") exceeds model context window (" + maxAllowedTokens + ")",
            "AI_CONTEXT_WINDOW_EXCEEDED",
            false, // Not retryable without prompt compaction/truncation
            modelName,
            Map.of("actualTokens", actualTokens, "maxAllowedTokens", maxAllowedTokens),
            null
        );
        this.actualTokens = actualTokens;
        this.maxAllowedTokens = maxAllowedTokens;
    }

    public int getActualTokens() { return actualTokens; }
    public int getMaxAllowedTokens() { return maxAllowedTokens; }
}
