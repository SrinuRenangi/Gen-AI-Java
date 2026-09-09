package code;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Enterprise Global Exception Handler simulating Spring's @RestControllerAdvice.
 *
 * Responsibilities:
 * 1. Intercept all exceptions thrown from any controller in the application.
 * 2. Map domain-specific AI exceptions to exact HTTP status codes (400, 429, 504, 500).
 * 3. Enrich error responses with Correlation IDs for distributed tracing.
 * 4. Mask sensitive internal database or stack trace data for unexpected errors (500).
 * 5. Format responses according to RFC 7807 Problem Details specification.
 */
public class GlobalExceptionHandler {

    public record ErrorResponse(
        int status,
        Map<String, Object> headers,
        String jsonBody
    ) {}

    /**
     * Handles ModelRateLimitException -> HTTP 429 Too Many Requests
     */
    public ErrorResponse handleRateLimit(ModelRateLimitException ex, String path) {
        String corrId = CorrelationContext.get();
        System.err.printf("[%s] [WARN] Rate limit hit on model '%s': %s (Retry after %ds)%n",
            corrId, ex.getModelName(), ex.getMessage(), ex.getRetryAfterSeconds());

        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/problem+json");
        headers.put("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        headers.put("X-Correlation-Id", corrId);

        String json = buildProblemJson(
            "https://api.enterprise-ai.internal/errors/rate-limit-exceeded",
            "Rate Limit Exceeded",
            429,
            ex.getMessage(),
            path,
            corrId,
            ex.getErrorCode(),
            ex.isRetryable(),
            Map.of("retryAfterSeconds", ex.getRetryAfterSeconds(), "model", ex.getModelName())
        );

        return new ErrorResponse(429, headers, json);
    }

    /**
     * Handles ContextWindowExceededException -> HTTP 400 Bad Request
     */
    public ErrorResponse handleContextExceeded(ContextWindowExceededException ex, String path) {
        String corrId = CorrelationContext.get();
        System.err.printf("[%s] [WARN] Context window exceeded for model '%s': actual=%d, max=%d%n",
            corrId, ex.getModelName(), ex.getActualTokens(), ex.getMaxAllowedTokens());

        Map<String, Object> headers = Map.of(
            "Content-Type", "application/problem+json",
            "X-Correlation-Id", corrId
        );

        String json = buildProblemJson(
            "https://api.enterprise-ai.internal/errors/context-window-exceeded",
            "Context Window Exceeded",
            400,
            ex.getMessage(),
            path,
            corrId,
            ex.getErrorCode(),
            ex.isRetryable(),
            Map.of(
                "actualTokens", ex.getActualTokens(),
                "maxAllowedTokens", ex.getMaxAllowedTokens(),
                "model", ex.getModelName()
            )
        );

        return new ErrorResponse(400, headers, json);
    }

    /**
     * Handles ModelProviderTimeoutException -> HTTP 504 Gateway Timeout
     */
    public ErrorResponse handleProviderTimeout(ModelProviderTimeoutException ex, String path) {
        String corrId = CorrelationContext.get();
        System.err.printf("[%s] [ERROR] Model inference timed out after %dms on model '%s'%n",
            corrId, ex.getTimeoutMs(), ex.getModelName());

        Map<String, Object> headers = Map.of(
            "Content-Type", "application/problem+json",
            "X-Correlation-Id", corrId
        );

        String json = buildProblemJson(
            "https://api.enterprise-ai.internal/errors/gateway-timeout",
            "Gateway Timeout",
            504,
            "Upstream AI model provider failed to respond within " + ex.getTimeoutMs() + "ms",
            path,
            corrId,
            ex.getErrorCode(),
            ex.isRetryable(),
            Map.of("timeoutMs", ex.getTimeoutMs(), "model", ex.getModelName())
        );

        return new ErrorResponse(504, headers, json);
    }

    /**
     * Catch-all fallback for unexpected internal errors -> HTTP 500 Internal Server Error
     *
     * SECURITY RULE: Never leak ex.getMessage() or stack trace to client!
     */
    public ErrorResponse handleUnexpected(Throwable ex, String path) {
        String corrId = CorrelationContext.get();
        // Log full stack trace internally with correlation ID for engineers to diagnose
        System.err.printf("[%s] [FATAL] Unexpected unhandled exception: %s: %s%n",
            corrId, ex.getClass().getName(), ex.getMessage());

        Map<String, Object> headers = Map.of(
            "Content-Type", "application/problem+json",
            "X-Correlation-Id", corrId
        );

        String json = buildProblemJson(
            "https://api.enterprise-ai.internal/errors/internal-server-error",
            "Internal Server Error",
            500,
            "An unexpected internal error occurred. Please quote correlation ID '" + corrId + "' to support.",
            path,
            corrId,
            "INTERNAL_ERROR",
            false,
            Map.of()
        );

        return new ErrorResponse(500, headers, json);
    }

    /**
     * Centralized RFC 7807 JSON builder.
     */
    private String buildProblemJson(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String correlationId,
        String errorCode,
        boolean retryable,
        Map<String, Object> extraProps
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"type\": \"").append(type).append("\",\n");
        sb.append("  \"title\": \"").append(title).append("\",\n");
        sb.append("  \"status\": ").append(status).append(",\n");
        sb.append("  \"detail\": \"").append(escape(detail)).append("\",\n");
        sb.append("  \"instance\": \"").append(instance).append("\",\n");
        sb.append("  \"correlationId\": \"").append(correlationId).append("\",\n");
        sb.append("  \"errorCode\": \"").append(errorCode).append("\",\n");
        sb.append("  \"retryable\": ").append(retryable).append(",\n");
        sb.append("  \"timestamp\": \"").append(Instant.now()).append("\"");

        if (!extraProps.isEmpty()) {
            sb.append(",\n  \"details\": {\n");
            int i = 0;
            for (Map.Entry<String, Object> entry : extraProps.entrySet()) {
                i++;
                sb.append("    \"").append(entry.getKey()).append("\": ");
                if (entry.getValue() instanceof Number || entry.getValue() instanceof Boolean) {
                    sb.append(entry.getValue());
                } else {
                    sb.append("\"").append(escape(String.valueOf(entry.getValue()))).append("\"");
                }
                if (i < extraProps.size()) sb.append(",");
                sb.append("\n");
            }
            sb.append("  }\n");
        } else {
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", "\\n");
    }
}
