package com.genai.security.apisec;

import java.util.*;

/**
 * Simulates Spring Security's CorsConfiguration and CorsFilter.
 * Validates origins, methods, and headers, and sets standard CORS response headers.
 */
public class CorsPolicyValidator {

    private final Set<String> allowedOrigins;
    private final Set<String> allowedMethods;
    private final Set<String> allowedHeaders;
    private final boolean allowCredentials;

    public CorsPolicyValidator(
            Set<String> allowedOrigins,
            Set<String> allowedMethods,
            Set<String> allowedHeaders,
            boolean allowCredentials
    ) {
        this.allowedOrigins = Collections.unmodifiableSet(new HashSet<>(allowedOrigins));
        this.allowedMethods = Collections.unmodifiableSet(new HashSet<>(allowedMethods));
        this.allowedHeaders = Collections.unmodifiableSet(new HashSet<>(allowedHeaders));
        this.allowCredentials = allowCredentials;
    }

    public record CorsValidationResult(
            boolean allowed,
            String failureReason,
            Map<String, String> responseHeaders
    ) {}

    public CorsValidationResult validateAndBuildHeaders(String origin, String method, String requestHeaders) {
        // Non-CORS request
        if (origin == null || origin.isBlank()) {
            return new CorsValidationResult(true, null, Map.of());
        }

        // Check origin whitelist
        if (!allowedOrigins.contains("*") && !allowedOrigins.contains(origin)) {
            return new CorsValidationResult(false, "CORS origin '" + origin + "' is not whitelisted", Map.of());
        }

        // Check method
        if (method != null && !allowedMethods.contains(method.toUpperCase())) {
            return new CorsValidationResult(false, "HTTP method '" + method + "' is not allowed by CORS", Map.of());
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Access-Control-Allow-Origin", allowedOrigins.contains("*") ? "*" : origin);
        headers.put("Access-Control-Allow-Methods", String.join(", ", allowedMethods));
        headers.put("Access-Control-Allow-Headers", String.join(", ", allowedHeaders));
        headers.put("Access-Control-Max-Age", "3600");
        if (allowCredentials && !allowedOrigins.contains("*")) {
            headers.put("Access-Control-Allow-Credentials", "true");
        }

        return new CorsValidationResult(true, null, headers);
    }
}
