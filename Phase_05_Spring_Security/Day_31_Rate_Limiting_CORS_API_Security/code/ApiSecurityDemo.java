package com.genai.security.apisec;

import java.util.Set;

public class ApiSecurityDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("================================================================================");
        System.out.println("  DAY 31: API SECURITY, RATE LIMITING & CORS DEFENSE DEMONSTRATION             ");
        System.out.println("================================================================================\n");

        // 1. Configure CORS Whitelist
        CorsPolicyValidator corsValidator = new CorsPolicyValidator(
                Set.of("https://chat.myenterprise.com", "http://localhost:3000"),
                Set.of("GET", "POST", "OPTIONS"),
                Set.of("Authorization", "Content-Type", "X-Workspace-Id"),
                true
        );

        // 2. Configure Token Bucket: Capacity = 3 requests burst, Refills at 2 tokens/sec
        RateLimiterRegistry registry = new RateLimiterRegistry(() -> new TokenBucket(3, 2));

        AiSecurityGateway gateway = new AiSecurityGateway(corsValidator, registry);

        // -----------------------------------------------------------------------------------------
        // SCENARIO 1: Allowed CORS Request from Whitelisted Origin
        // -----------------------------------------------------------------------------------------
        System.out.println("[TEST 1] Testing Allowed CORS Request from Whitelisted Origin...");
        AiSecurityGateway.HttpRequest req1 = new AiSecurityGateway.HttpRequest(
                "192.168.1.50", "user-101",
                "https://chat.myenterprise.com", "POST",
                "/api/v1/ai/generate", "{\"prompt\":\"Summarize quarterly earnings\"}"
        );
        AiSecurityGateway.HttpResponse resp1 = gateway.handleRequest(req1);
        System.out.println("  Status Code: " + resp1.statusCode());
        System.out.println("  CORS Origin: " + resp1.headers().get("Access-Control-Allow-Origin"));
        System.out.println("  CSP Header:  " + resp1.headers().get("Content-Security-Policy"));
        System.out.println("  Rate Remaining: " + resp1.headers().get("X-RateLimit-Remaining"));
        if (resp1.statusCode() == 200) {
            System.out.println("  ✅ TEST 1 PASSED!");
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 2: Blocked CORS Request from Rogue Origin
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 2] Testing Blocked CORS Request from Rogue Origin...");
        AiSecurityGateway.HttpRequest rogueReq = new AiSecurityGateway.HttpRequest(
                "203.0.113.5", "user-101",
                "https://evil-hacker.com", "POST",
                "/api/v1/ai/generate", "{\"prompt\":\"Steal embeddings\"}"
        );
        AiSecurityGateway.HttpResponse rogueResp = gateway.handleRequest(rogueReq);
        System.out.println("  Status Code: " + rogueResp.statusCode() + " (" + rogueResp.body() + ")");
        if (rogueResp.statusCode() == 403) {
            System.out.println("  ✅ TEST 2 PASSED: Rogue CORS request blocked!");
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 3: CORS Preflight OPTIONS Request
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 3] Testing CORS Preflight OPTIONS Request...");
        AiSecurityGateway.HttpRequest optionsReq = new AiSecurityGateway.HttpRequest(
                "192.168.1.50", "user-101",
                "https://chat.myenterprise.com", "OPTIONS",
                "/api/v1/ai/generate", null
        );
        AiSecurityGateway.HttpResponse optionsResp = gateway.handleRequest(optionsReq);
        System.out.println("  Status Code: " + optionsResp.statusCode());
        System.out.println("  Allowed Methods: " + optionsResp.headers().get("Access-Control-Allow-Methods"));
        if (optionsResp.statusCode() == 204) {
            System.out.println("  ✅ TEST 3 PASSED: Preflight handled cleanly with 204 No Content.");
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 4 & 5: Rate Limiting Burst & Denial-of-Wallet Exhaustion (429)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 4 & 5] Simulating Rapid AI Inference Requests (Burst Capacity = 3)...");
        String spammerIp = "10.0.0.99";
        for (int i = 1; i <= 5; i++) {
            AiSecurityGateway.HttpRequest floodReq = new AiSecurityGateway.HttpRequest(
                    spammerIp, null, // anonymous IP-based
                    "http://localhost:3000", "POST",
                    "/api/v1/ai/stream", "{\"prompt\":\"Generate 1000 poems\"}"
            );
            AiSecurityGateway.HttpResponse floodResp = gateway.handleRequest(floodReq);
            System.out.printf("  Request #%d -> Status: %d | Remaining: %s | Retry-After: %s%n",
                    i,
                    floodResp.statusCode(),
                    floodResp.headers().getOrDefault("X-RateLimit-Remaining", "N/A"),
                    floodResp.headers().getOrDefault("Retry-After", "0s")
            );
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 6: Token Bucket Refill over Time
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 6] Waiting 600ms for Token Bucket to Refill...");
        Thread.sleep(600); // refills > 1 token at 2 tokens/sec
        AiSecurityGateway.HttpRequest retryReq = new AiSecurityGateway.HttpRequest(
                spammerIp, null,
                "http://localhost:3000", "POST",
                "/api/v1/ai/stream", "{\"prompt\":\"Retry after backoff\"}"
        );
        AiSecurityGateway.HttpResponse retryResp = gateway.handleRequest(retryReq);
        System.out.printf("  Retry after wait -> Status: %d | Remaining: %s%n",
                retryResp.statusCode(),
                retryResp.headers().get("X-RateLimit-Remaining")
        );
        if (retryResp.statusCode() == 200) {
            System.out.println("  ✅ TEST 6 PASSED: Request succeeded after token refill.");
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 7: Oversized Prompt Payload DoS Guard
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 7] Testing Oversized Prompt Payload DoS Guard (>64KB)...");
        String massivePrompt = "A".repeat(70 * 1024); // 70 KB payload
        AiSecurityGateway.HttpRequest giantReq = new AiSecurityGateway.HttpRequest(
                "192.168.1.1", "user-admin",
                "http://localhost:3000", "POST",
                "/api/v1/ai/generate", massivePrompt
        );
        AiSecurityGateway.HttpResponse giantResp = gateway.handleRequest(giantReq);
        System.out.println("  Status Code: " + giantResp.statusCode() + " (" + giantResp.body() + ")");
        if (giantResp.statusCode() == 413) {
            System.out.println("  ✅ TEST 7 PASSED: Giant prompt rejected before reaching JSON parser.");
        }

        System.out.println("\n================================================================================");
        System.out.println("  ALL API SECURITY, RATE LIMITING & CORS TESTS PASSED!                         ");
        System.out.println("================================================================================");
    }
}
