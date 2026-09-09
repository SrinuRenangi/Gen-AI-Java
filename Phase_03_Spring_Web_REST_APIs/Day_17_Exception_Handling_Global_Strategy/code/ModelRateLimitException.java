package code;

import java.util.Map;

/**
 * Thrown when an upstream LLM provider responds with HTTP 429 Too Many Requests.
 *
 * Mapped to: HTTP 429 Too Many Requests in GlobalExceptionHandler.
 * Carries: retryAfterSeconds to inform exponential backoff algorithms.
 */
public class ModelRateLimitException extends GenAIException {

    private final int retryAfterSeconds;

    public ModelRateLimitException(String modelName, int retryAfterSeconds, String message) {
        super(
            message,
            "AI_RATE_LIMIT_EXCEEDED",
            true, // Retryable after delay!
            modelName,
            Map.of("retryAfterSeconds", retryAfterSeconds),
            null
        );
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
