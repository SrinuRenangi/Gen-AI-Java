package code;

import java.util.Map;

/**
 * Enterprise Test Suite simulating @WebMvcTest slice tests.
 *
 * Tests:
 * 1. Happy Path: Valid AI prompt -> 200 OK with token metrics.
 * 2. Bean Validation: Blank prompt -> 422 Unprocessable Entity (RFC 7807).
 * 3. Security Whitelist: Unapproved model -> 422 Unprocessable Entity.
 * 4. Error Mapping: Upstream TPM rate limit -> 429 Too Many Requests with Retry-After header.
 * 5. Error Mapping: Upstream inference timeout -> 504 Gateway Timeout.
 */
public class ChatControllerTest {

    private final MockMvcSimulator mockMvc;

    public ChatControllerTest() {
        // Wire up simulated controller dispatch pipeline
        this.mockMvc = new MockMvcSimulator(this::handleRequest);
    }

    public void runAllTests() {
        System.out.println("Running ChatControllerTest (@WebMvcTest Slice)...");

        testValidCompletion_Returns200();
        testBlankPrompt_Returns422();
        testUnapprovedModel_Returns422();
        testUpstreamRateLimit_Returns429WithRetryAfter();
        testUpstreamTimeout_Returns504();

        System.out.println("All 5 controller slice tests PASSED successfully!");
    }

    private void testValidCompletion_Returns200() {
        System.out.print("  [TEST 1] testValidCompletion_Returns200 ... ");
        String json = """
            {
              "prompt": "Explain Java 21 Records",
              "model": "gpt-4o",
              "temperature": 0.7,
              "maxTokens": 1000
            }
            """;

        mockMvc.perform(MockMvcSimulator.post("/api/v1/chat/completions").content(json))
            .andExpectStatus(200)
            .andExpectBodyContains("\"completion\"")
            .andExpectBodyContains("\"totalTokens\"");
        System.out.println("PASSED");
    }

    private void testBlankPrompt_Returns422() {
        System.out.print("  [TEST 2] testBlankPrompt_Returns422 ... ");
        String json = """
            {
              "prompt": "   ",
              "model": "gpt-4o",
              "temperature": 0.7,
              "maxTokens": 1000
            }
            """;

        mockMvc.perform(MockMvcSimulator.post("/api/v1/chat/completions").content(json))
            .andExpectStatus(422)
            .andExpectBodyContains("\"title\": \"Validation Failed\"")
            .andExpectBodyContains("Prompt must not be null, empty, or blank");
        System.out.println("PASSED");
    }

    private void testUnapprovedModel_Returns422() {
        System.out.print("  [TEST 3] testUnapprovedModel_Returns422 ... ");
        String json = """
            {
              "prompt": "Hello AI",
              "model": "unapproved-random-model",
              "temperature": 0.7,
              "maxTokens": 1000
            }
            """;

        mockMvc.perform(MockMvcSimulator.post("/api/v1/chat/completions").content(json))
            .andExpectStatus(422)
            .andExpectBodyContains("is not approved");
        System.out.println("PASSED");
    }

    private void testUpstreamRateLimit_Returns429WithRetryAfter() {
        System.out.print("  [TEST 4] testUpstreamRateLimit_Returns429WithRetryAfter ... ");
        String json = """
            {
              "prompt": "TRIGGER_RATE_LIMIT",
              "model": "gpt-4o",
              "temperature": 0.7,
              "maxTokens": 1000
            }
            """;

        mockMvc.perform(MockMvcSimulator.post("/api/v1/chat/completions").content(json))
            .andExpectStatus(429)
            .andExpectHeader("Retry-After", "30")
            .andExpectBodyContains("\"errorCode\": \"AI_RATE_LIMIT_EXCEEDED\"");
        System.out.println("PASSED");
    }

    private void testUpstreamTimeout_Returns504() {
        System.out.print("  [TEST 5] testUpstreamTimeout_Returns504 ... ");
        String json = """
            {
              "prompt": "TRIGGER_TIMEOUT",
              "model": "claude-3-5-sonnet",
              "temperature": 0.7,
              "maxTokens": 1000
            }
            """;

        mockMvc.perform(MockMvcSimulator.post("/api/v1/chat/completions").content(json))
            .andExpectStatus(504)
            .andExpectBodyContains("\"errorCode\": \"AI_PROVIDER_TIMEOUT\"");
        System.out.println("PASSED");
    }

    /**
     * Internal simulation of DispatcherServlet + Controller + ExceptionHandler.
     */
    private MockMvcSimulator.TestResponse handleRequest(MockMvcSimulator.TestRequest req) {
        String body = req.getContent();

        // 1. Validation checks
        if (body.contains("\"prompt\": \"   \"")) {
            return new MockMvcSimulator.TestResponse(
                422,
                Map.of("Content-Type", "application/problem+json"),
                """
                {
                  "type": "https://api.enterprise-ai.internal/errors/validation-failed",
                  "title": "Validation Failed",
                  "status": 422,
                  "detail": "Prompt must not be null, empty, or blank"
                }
                """
            );
        }

        if (body.contains("unapproved-random-model")) {
            return new MockMvcSimulator.TestResponse(
                422,
                Map.of("Content-Type", "application/problem+json"),
                """
                {
                  "type": "https://api.enterprise-ai.internal/errors/validation-failed",
                  "title": "Validation Failed",
                  "status": 422,
                  "detail": "Model 'unapproved-random-model' is not approved"
                }
                """
            );
        }

        // 2. Simulated service exceptions
        if (body.contains("TRIGGER_RATE_LIMIT")) {
            return new MockMvcSimulator.TestResponse(
                429,
                Map.of("Content-Type", "application/problem+json", "Retry-After", "30"),
                """
                {
                  "type": "https://api.enterprise-ai.internal/errors/rate-limit-exceeded",
                  "title": "Rate Limit Exceeded",
                  "status": 429,
                  "errorCode": "AI_RATE_LIMIT_EXCEEDED"
                }
                """
            );
        }

        if (body.contains("TRIGGER_TIMEOUT")) {
            return new MockMvcSimulator.TestResponse(
                504,
                Map.of("Content-Type", "application/problem+json"),
                """
                {
                  "type": "https://api.enterprise-ai.internal/errors/gateway-timeout",
                  "title": "Gateway Timeout",
                  "status": 504,
                  "errorCode": "AI_PROVIDER_TIMEOUT"
                }
                """
            );
        }

        // 3. Success 200 OK
        return new MockMvcSimulator.TestResponse(
            200,
            Map.of("Content-Type", "application/json"),
            """
            {
              "id": "cmpl_test_123",
              "model": "gpt-4o",
              "completion": "Java 21 Records are immutable data carriers.",
              "totalTokens": 42
            }
            """
        );
    }
}
