package code;

import java.util.List;
import java.util.Map;

/**
 * Driver class demonstrating Day 19: API Documentation & OpenAPI (Swagger / SpringDoc).
 *
 * Demonstrates:
 * 1. Generating enterprise OpenAPI 3.1 specification for AI endpoints.
 * 2. Incorporating schema constraints (@Schema, min, max, enum, required).
 * 3. Exporting OpenAI / Claude compatible Tool-Calling schemas from the API specification.
 */
public class OpenApiDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DAY 19: API DOCUMENTATION & OPENAPI (SWAGGER / SPRINGDOC) IN GEN AI GATEWAYS   ");
        System.out.println("================================================================================");

        OpenApiSchemaGenerator generator = new OpenApiSchemaGenerator();

        // 1. API Metadata
        ChatCompletionApiSpec.ApiInfo info = new ChatCompletionApiSpec.ApiInfo(
            "Enterprise Gen AI Gateway API",
            "v1.4.0",
            "High-concurrency LLM inference, RAG embeddings, and prompt orchestration gateway.",
            "ai-platform@enterprise.internal",
            "Apache 2.0"
        );

        // 2. Server environments
        List<ChatCompletionApiSpec.ServerInfo> servers = List.of(
            new ChatCompletionApiSpec.ServerInfo("https://ai-gateway.internal.corp/api/v1", "Production Kubernetes Cluster"),
            new ChatCompletionApiSpec.ServerInfo("http://localhost:8080/api/v1", "Local Developer Sandbox")
        );

        // 3. Schema properties for CompletionRequest
        List<ChatCompletionApiSpec.SchemaProperty> completionRequestProps = List.of(
            new ChatCompletionApiSpec.SchemaProperty(
                "prompt",
                "string",
                "User prompt to be completed by the LLM.",
                "Explain the difference between Stack and Heap memory in Java 21.",
                true,
                null,
                null,
                null
            ),
            new ChatCompletionApiSpec.SchemaProperty(
                "model",
                "string",
                "The corporate approved foundation model name.",
                "gpt-4o",
                true,
                List.of("gpt-4o", "gpt-4o-mini", "claude-3-5-sonnet", "llama3.2", "mistral-large"),
                null,
                null
            ),
            new ChatCompletionApiSpec.SchemaProperty(
                "temperature",
                "number",
                "Sampling temperature between 0.0 (deterministic) and 2.0 (creative).",
                0.7,
                false,
                null,
                0.0,
                2.0
            ),
            new ChatCompletionApiSpec.SchemaProperty(
                "maxTokens",
                "integer",
                "Maximum number of tokens to generate.",
                1024,
                false,
                null,
                1.0,
                4096.0
            )
        );

        // 4. Operations
        List<ChatCompletionApiSpec.OperationDoc> operations = List.of(
            new ChatCompletionApiSpec.OperationDoc(
                "POST",
                "/chat/completions",
                "Create Chat Completion",
                "Dispatches a validated prompt to the configured LLM provider and returns completion statistics.",
                "Inference",
                "CompletionRequest",
                Map.of(
                    200, "Successful completion generated",
                    400, "Bad Request: Context window exceeded or invalid JSON",
                    422, "Validation Error: Field constraint violation (RFC 7807)",
                    429, "Too Many Requests: Rate limit exceeded (Retry-After header present)",
                    504, "Gateway Timeout: Upstream LLM inference timed out"
                )
            )
        );

        // -------------------------------------------------------------------------
        // PART 1: Output OpenAPI 3.1 Specification
        // -------------------------------------------------------------------------
        System.out.println("\n--- PART 1: Generated OpenAPI 3.1 JSON Specification ---");
        String openApiJson = generator.generateOpenApiJson(
            info,
            servers,
            operations,
            Map.of("CompletionRequest", completionRequestProps)
        );
        System.out.println(openApiJson);

        // -------------------------------------------------------------------------
        // PART 2: LLM Tool-Calling Function Schema Export
        // -------------------------------------------------------------------------
        System.out.println("\n--- PART 2: LLM Tool-Calling Function Schema (OpenAI / Claude Compatible) ---");
        String toolJson = generator.generateToolCallingSchema(
            "generate_ai_completion",
            "Generate text completion using approved enterprise LLMs.",
            completionRequestProps
        );
        System.out.println(toolJson);

        System.out.println("\n================================================================================");
        System.out.println(" DAY 19 DEMONSTRATION COMPLETE: OPENAPI & TOOL SCHEMAS SYNCHRONIZED!            ");
        System.out.println("================================================================================");
    }
}
