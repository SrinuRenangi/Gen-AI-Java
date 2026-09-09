package code;

import java.util.List;
import java.util.Optional;

/**
 * Driver class demonstrating Day 16: Request Validation, DTOs & Response Design.
 *
 * Demonstrates:
 * 1. Valid AI completion request -> 200 OK with clean DTO response and token usage.
 * 2. Multi-field invalid request -> 422 Unprocessable Entity with RFC 7807 Problem Details.
 * 3. Cross-field validation failure (RAG chunking: overlap >= chunkSize).
 * 4. Valid boundary conditions (temperature 0.0, max tokens 4096).
 */
public class ValidationDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DAY 16: REQUEST VALIDATION, DTOs & RESPONSE DESIGN (RFC 7807 PROBLEM DETAILS)  ");
        System.out.println("================================================================================");

        ValidationEngine engine = new ValidationEngine();

        // -------------------------------------------------------------------------
        // SCENARIO 1: Valid AI Completion Request
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 1: Valid AI Completion Request ---");
        CompletionRequest validReq = new CompletionRequest(
            "Explain how Java 21 Virtual Threads optimize I/O-bound LLM API calls.",
            "gpt-4o",
            0.7,
            1500,
            "You are a senior Java performance architect.",
            List.of("Human:", "Assistant:")
        );

        List<ValidationEngine.Violation> violations1 = engine.validateCompletionRequest(validReq);
        if (violations1.isEmpty()) {
            System.out.println(" [SUCCESS] Validation Passed! Processing AI completion...");
            // Simulate AI service response
            CompletionResponse response = CompletionResponse.success(
                "cmpl_9a8b7c6d",
                validReq.model(),
                validReq.prompt(),
                "Virtual threads unmount from carrier threads during socket blocking, allowing millions of concurrent LLM calls.",
                238
            );
            System.out.println(" HTTP Status: 200 OK");
            System.out.println(" Response Body:");
            System.out.println("   ID: " + response.id());
            System.out.println("   Model: " + response.model());
            System.out.println("   Completion: \"" + response.completion() + "\"");
            System.out.println("   Prompt Tokens: " + response.usage().promptTokens());
            System.out.println("   Completion Tokens: " + response.usage().completionTokens());
            System.out.println("   Total Tokens: " + response.usage().totalTokens());
            System.out.println("   Latency: " + response.latencyMs() + "ms");
            System.out.println("   Timestamp: " + response.timestamp());
        }

        // -------------------------------------------------------------------------
        // SCENARIO 2: Multi-Field Validation Failure -> RFC 7807 Problem Details
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 2: Multi-Field Validation Failure (422 Unprocessable Entity) ---");
        CompletionRequest invalidReq = new CompletionRequest(
            "   ",                           // VIOLATION: blank prompt
            "unapproved-gpt-2",              // VIOLATION: not in enterprise whitelist
            3.5,                             // VIOLATION: temperature > 2.0
            -10,                             // VIOLATION: maxTokens < 1
            null,
            List.of("1", "2", "3", "4", "5") // VIOLATION: more than 4 stop sequences
        );

        List<ValidationEngine.Violation> violations2 = engine.validateCompletionRequest(invalidReq);
        Optional<ProblemDetail> problem2 = engine.toProblemDetail(violations2, "/api/v1/chat/completions");

        if (problem2.isPresent()) {
            System.out.println(" [VALIDATION REJECTED] Found " + violations2.size() + " violations.");
            System.out.println(" Content-Type: application/problem+json");
            System.out.println(" HTTP Status: " + problem2.get().getStatus());
            System.out.println("\n RFC 7807 Problem Detail Payload:");
            System.out.println(problem2.get().toJson());
        }

        // -------------------------------------------------------------------------
        // SCENARIO 3: Cross-Field Validation (RAG Chunking overlap >= chunkSize)
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 3: Cross-Field Validation Failure in RAG Pipeline ---");
        RagChunkingRequest brokenRag = new RagChunkingRequest(
            "doc_legal_contract_2026",
            500,  // chunkSize
            600,  // chunkOverlap > chunkSize (BROKEN: sliding window step is negative!)
            "RECURSIVE_CHARACTER"
        );

        List<ValidationEngine.Violation> violations3 = engine.validateRagChunking(brokenRag);
        Optional<ProblemDetail> problem3 = engine.toProblemDetail(violations3, "/api/v1/rag/documents/chunk");

        if (problem3.isPresent()) {
            System.out.println(" [VALIDATION REJECTED] Cross-field constraint violation detected!");
            System.out.println(" HTTP Status: " + problem3.get().getStatus());
            System.out.println(problem3.get().toJson());
        }

        // -------------------------------------------------------------------------
        // SCENARIO 4: Valid Boundary Conditions
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 4: Valid Boundary Conditions (temp=0.0, maxTokens=4096) ---");
        CompletionRequest boundaryReq = new CompletionRequest(
            "Translate the following SQL query to JPQL.",
            "claude-3-5-sonnet",
            0.0,   // Lowest valid temperature
            4096,  // Highest valid token limit
            null,
            null
        );

        List<ValidationEngine.Violation> violations4 = engine.validateCompletionRequest(boundaryReq);
        if (violations4.isEmpty()) {
            System.out.println(" [SUCCESS] Boundary conditions accepted! Temperature: " 
                + boundaryReq.temperature() + ", MaxTokens: " + boundaryReq.maxTokens());
        } else {
            System.out.println(" [ERROR] Unexpected violations: " + violations4.size());
        }

        System.out.println("\n================================================================================");
        System.out.println(" DAY 16 DEMONSTRATION COMPLETE: ALL ARCHITECTURAL CONSTRAINTS VERIFIED!         ");
        System.out.println("================================================================================");
    }
}
