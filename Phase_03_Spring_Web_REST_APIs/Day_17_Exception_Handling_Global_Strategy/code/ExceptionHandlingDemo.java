package code;

/**
 * Driver class demonstrating Day 17: Exception Handling & Global Error Strategy.
 *
 * Demonstrates:
 * 1. Distributed Correlation Context propagation (MDC pattern).
 * 2. Specialized exception handling for LLM failure modes (429, 400, 504).
 * 3. Sanitized fallback handling for unexpected exceptions (500 without stack leakage).
 * 4. RFC 7807 problem details with correlation telemetry.
 */
public class ExceptionHandlingDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DAY 17: EXCEPTION HANDLING & GLOBAL ERROR STRATEGY IN GEN AI SYSTEMS          ");
        System.out.println("================================================================================");

        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        // -------------------------------------------------------------------------
        // SCENARIO 1: Model Rate Limit Exceeded (HTTP 429)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 1: Upstream Model Rate Limit (HTTP 429) ---");
        CorrelationContext.init("req_openai_rate_9812");
        try {
            // Simulate upstream service throwing ModelRateLimitException
            throw new ModelRateLimitException(
                "gpt-4o",
                30,
                "OpenAI TPM (Tokens Per Minute) threshold of 250,000 exceeded for organization org_corp_123"
            );
        } catch (ModelRateLimitException ex) {
            GlobalExceptionHandler.ErrorResponse response = handler.handleRateLimit(ex, "/api/v1/chat/completions");
            printResponse(response);
        } finally {
            CorrelationContext.clear();
        }

        // -------------------------------------------------------------------------
        // SCENARIO 2: Context Window Exceeded (HTTP 400)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 2: Context Window Exceeded (HTTP 400) ---");
        CorrelationContext.init("req_rag_oversize_4421");
        try {
            // Simulate RAG pipeline assembling 145,000 tokens for a 128,000 token model
            throw new ContextWindowExceededException("gpt-4o", 145200, 128000);
        } catch (ContextWindowExceededException ex) {
            GlobalExceptionHandler.ErrorResponse response = handler.handleContextExceeded(ex, "/api/v1/rag/query");
            printResponse(response);
        } finally {
            CorrelationContext.clear();
        }

        // -------------------------------------------------------------------------
        // SCENARIO 3: Provider Gateway Timeout (HTTP 504)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 3: Model Provider Inference Timeout (HTTP 504) ---");
        CorrelationContext.init("req_anthropic_slow_7719");
        try {
            // Simulate reasoning model taking > 60 seconds
            throw new ModelProviderTimeoutException(
                "claude-3-5-sonnet",
                60000,
                "Inference request timed out after waiting 60,000ms for first token"
            );
        } catch (ModelProviderTimeoutException ex) {
            GlobalExceptionHandler.ErrorResponse response = handler.handleProviderTimeout(ex, "/api/v1/chat/completions");
            printResponse(response);
        } finally {
            CorrelationContext.clear();
        }

        // -------------------------------------------------------------------------
        // SCENARIO 4: Security-Critical Fallback: Unexpected Database NullPointer (HTTP 500)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 4: Unexpected Exception Scrubbing (HTTP 500) ---");
        CorrelationContext.init("req_db_crash_1104");
        try {
            // Simulate unexpected internal database error containing secret connection URL
            throw new RuntimeException("SQLException: Connection to postgres://admin:superSecretP@ssword@db.internal:5432 failed");
        } catch (Throwable ex) {
            GlobalExceptionHandler.ErrorResponse response = handler.handleUnexpected(ex, "/api/v1/prompts/templates");
            printResponse(response);
        } finally {
            CorrelationContext.clear();
        }

        System.out.println("\n================================================================================");
        System.out.println(" DAY 17 DEMONSTRATION COMPLETE: ALL EXCEPTION FLOWS VERIFIED & AUDITED!         ");
        System.out.println("================================================================================");
    }

    private static void printResponse(GlobalExceptionHandler.ErrorResponse resp) {
        System.out.println(" Status: " + resp.status());
        System.out.println(" Headers: " + resp.headers());
        System.out.println(" Payload (application/problem+json):");
        System.out.println(resp.jsonBody());
    }
}
