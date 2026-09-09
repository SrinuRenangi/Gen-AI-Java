package code;

import java.util.Map;

/**
 * Thrown when an upstream LLM API call times out during inference.
 *
 * Mapped to: HTTP 504 Gateway Timeout.
 * Retryable: Transient network glitch or high provider GPU load.
 */
public class ModelProviderTimeoutException extends GenAIException {

    private final long timeoutMs;

    public ModelProviderTimeoutException(String modelName, long timeoutMs, String message) {
        super(
            message,
            "AI_PROVIDER_TIMEOUT",
            true, // Retryable
            modelName,
            Map.of("timeoutMs", timeoutMs),
            null
        );
        this.timeoutMs = timeoutMs;
    }

    public long getTimeoutMs() { return timeoutMs; }
}
