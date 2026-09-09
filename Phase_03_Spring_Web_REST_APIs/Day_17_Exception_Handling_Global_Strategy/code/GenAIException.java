package code;

import java.util.Collections;
import java.util.Map;

/**
 * Base abstract runtime exception for all enterprise Generative AI errors.
 *
 * In production AI systems, exceptions must carry structured telemetry:
 * 1. errorCode: Machine-readable enum/string for client SDK error discrimination.
 * 2. retryable: Informs upstream clients or resilience circuits if retrying is safe.
 * 3. modelName: Identifies which LLM or embedding provider caused the failure.
 * 4. metadata: Contextual key-value pairs (e.g., token count, latency, provider status).
 */
public abstract class GenAIException extends RuntimeException {

    private final String errorCode;
    private final boolean retryable;
    private final String modelName;
    private final Map<String, Object> metadata;

    public GenAIException(
        String message,
        String errorCode,
        boolean retryable,
        String modelName,
        Map<String, Object> metadata,
        Throwable cause
    ) {
        super(message, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.modelName = modelName;
        this.metadata = metadata != null ? Collections.unmodifiableMap(metadata) : Collections.emptyMap();
    }

    public GenAIException(String message, String errorCode, boolean retryable, String modelName) {
        this(message, errorCode, retryable, modelName, Collections.emptyMap(), null);
    }

    public String getErrorCode() { return errorCode; }
    public boolean isRetryable() { return retryable; }
    public String getModelName() { return modelName; }
    public Map<String, Object> getMetadata() { return metadata; }
}
