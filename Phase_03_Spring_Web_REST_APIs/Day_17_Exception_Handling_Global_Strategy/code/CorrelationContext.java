package code;

import java.util.UUID;

/**
 * Enterprise Correlation Context (MDC simulation).
 *
 * In distributed microservice and AI systems:
 * 1. Every incoming HTTP request carries or receives an X-Correlation-ID.
 * 2. Every log line emitted during that request prints the correlation ID.
 * 3. Every downstream exception and error response returns the correlation ID.
 *
 * When an enterprise customer files a support ticket:
 * "Error: Context exceeded for correlation ID 7f3b-4c91"
 * Developers can grep production logs across 50 server pods instantly.
 */
public final class CorrelationContext {

    private static final ThreadLocal<String> CURRENT_CORRELATION_ID = new ThreadLocal<>();

    private CorrelationContext() {}

    /**
     * Initializes correlation ID from header or generates a new one.
     */
    public static String init(String headerValue) {
        String id = (headerValue != null && !headerValue.isBlank())
            ? headerValue.trim()
            : "corr_" + UUID.randomUUID().toString().substring(0, 8);
        CURRENT_CORRELATION_ID.set(id);
        return id;
    }

    public static String get() {
        String id = CURRENT_CORRELATION_ID.get();
        return id != null ? id : "corr_system";
    }

    public static void clear() {
        CURRENT_CORRELATION_ID.remove();
    }
}
