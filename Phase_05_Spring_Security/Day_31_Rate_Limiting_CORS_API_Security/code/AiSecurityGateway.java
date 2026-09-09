package com.genai.security.apisec;

import java.util.HashMap;
import java.util.Map;

/**
 * Simulates Spring Security Filter Chain for AI Endpoints:
 * - CORS Filtering
 * - OWASP Security Headers Injection
 * - Token-Bucket Rate Limiting (Bucket4j)
 */
public class AiSecurityGateway {

    public record HttpRequest(
            String clientIp,
            String userId,
            String origin,
            String method,
            String uri,
            String body
    ) {}

    public record HttpResponse(
            int statusCode,
            String body,
            Map<String, String> headers
    ) {}

    private final CorsPolicyValidator corsValidator;
    private final RateLimiterRegistry rateLimiterRegistry;

    public AiSecurityGateway(CorsPolicyValidator corsValidator, RateLimiterRegistry rateLimiterRegistry) {
        this.corsValidator = corsValidator;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    public HttpResponse handleRequest(HttpRequest request) {
        Map<String, String> responseHeaders = new HashMap<>();

        // 1. Injcet OWASP Defense-in-Depth Security Headers
        responseHeaders.put("X-Content-Type-Options", "nosniff");
        responseHeaders.put("X-Frame-Options", "DENY");
        responseHeaders.put("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        responseHeaders.put("Content-Security-Policy", "default-src 'self'");

        // 2. Process CORS Policy
        CorsPolicyValidator.CorsValidationResult corsResult = corsValidator.validateAndBuildHeaders(
                request.origin(),
                request.method(),
                null
        );

        if (!corsResult.allowed()) {
            return new HttpResponse(403, "CORS Error: " + corsResult.failureReason(), responseHeaders);
        }
        responseHeaders.putAll(corsResult.responseHeaders());

        // Handle preflight OPTIONS request
        if ("OPTIONS".equalsIgnoreCase(request.method())) {
            return new HttpResponse(204, "", responseHeaders);
        }

        // 3. Body Size Validation (Guarding against massive Prompt Injection payload DoS)
        if (request.body() != null && request.body().length() > 64 * 1024) { // 64KB max prompt
            return new HttpResponse(413, "Payload Too Large: Maximum prompt size is 64KB", responseHeaders);
        }

        // 4. Rate Limiting (Bucket4j token bucket per User or IP)
        String rateLimitKey = request.userId() != null ? "user:" + request.userId() : "ip:" + request.clientIp();
        TokenBucket bucket = rateLimiterRegistry.resolveBucket(rateLimitKey);

        responseHeaders.put("X-RateLimit-Limit", String.valueOf(bucket.getCapacity()));

        boolean consumed = bucket.tryConsume(1);
        if (!consumed) {
            long retryAfterSeconds = bucket.getSecondsUntilNextToken();
            responseHeaders.put("Retry-After", String.valueOf(retryAfterSeconds));
            responseHeaders.put("X-RateLimit-Remaining", "0");
            return new HttpResponse(
                    429,
                    "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please retry in " + retryAfterSeconds + "s.\"}",
                    responseHeaders
            );
        }

        responseHeaders.put("X-RateLimit-Remaining", String.valueOf(bucket.getAvailableTokens()));

        // Request passes security guards -> Ready for LLM Controller
        return new HttpResponse(
                200,
                "{\"status\":\"SUCCESS\",\"response\":\"AI response generated successfully for " + request.uri() + "\"}",
                responseHeaders
        );
    }
}
