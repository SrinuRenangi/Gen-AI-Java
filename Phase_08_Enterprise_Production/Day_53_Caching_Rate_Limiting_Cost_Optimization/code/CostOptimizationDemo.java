package com.genai.enterprise.costopt;

/**
 * Enterprise verification test driver for AI Caching, Rate Limiting, and Cost Optimization.
 */
public class CostOptimizationDemo {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("       ENTERPRISE AI COST OPTIMIZATION & RATE LIMITING SUITE             ");
        System.out.println("==========================================================================\n");

        // 60 requests/min, 2,000 tokens/min limit, 0.85 semantic similarity threshold
        CostOptimizationEngine engine = new CostOptimizationEngine(60, 2000, 0.85);

        // Test Scenario 1: Initial query (Cold LLM execution via dynamic model routing)
        String query1 = "Explain Spring Boot dependency injection";
        System.out.println("[Request 1: Cold Query] -> '" + query1 + "'");
        var res1 = engine.execute(query1);
        printResult(res1);

        // Test Scenario 2: Exact identical query (Exact Cache Hit)
        System.out.println("[Request 2: Exact Match Query] -> '" + query1 + "'");
        var res2 = engine.execute(query1);
        printResult(res2);

        // Test Scenario 3: Paraphrased query with same vocabulary (Semantic Cache Hit)
        String query3 = "Explain dependency injection in Spring Boot";
        System.out.println("[Request 3: Paraphrased Query] -> '" + query3 + "'");
        var res3 = engine.execute(query3);
        printResult(res3);

        // Test Scenario 4: High-complexity query routing to Tier 1 Frontier
        String query4 = "Architect a distributed consensus algorithm with zero-deadlock guarantee";
        System.out.println("[Request 4: Complex Architecture Query] -> '" + query4 + "'");
        var res4 = engine.execute(query4);
        printResult(res4);

        // Test Scenario 5: Short conversational query routing to Local Edge (Llama 3.2)
        String query5 = "Hello there";
        System.out.println("[Request 5: Short Chat Query] -> '" + query5 + "'");
        var res5 = engine.execute(query5);
        printResult(res5);

        // Test Scenario 6: Rate Limiting Enforcement (Exceeding TPM)
        System.out.println("[Request 6: Rate Limiting Attack Simulation]");
        try {
            // Request large prompt that exceeds token bucket headroom
            StringBuilder largePrompt = new StringBuilder();
            for (int i = 0; i < 400; i++) largePrompt.append("Token blast overflow payload test ");
            engine.execute(largePrompt.toString());
            System.out.println("FAILURE: Request unexpectedly succeeded");
        } catch (RuntimeException e) {
            System.out.println("SUCCESS: Rate Limiter intercepted overload -> " + e.getMessage() + "\n");
        }

        System.out.println("==========================================================================");
        System.out.println("                     FINAL COST METRICS REPORT                            ");
        System.out.println("==========================================================================");
        System.out.println("Exact Cache Hits    : " + engine.getExactCache().getHits());
        System.out.println("Semantic Cache Hits : " + engine.getSemanticCache().getSemanticHits());
        System.out.printf("Total Budget Saved  : $%.6f USD%n", engine.getTotalSavedUsd());
        System.out.println("==========================================================================");
        System.out.println(">>> Cost optimization and rate limiting verification completed successfully!");
    }

    private static void printResult(CostOptimizationEngine.ExecutionResult res) {
        System.out.printf("  Source     : %s%n", res.source());
        System.out.printf("  Model Used : %s%n", res.modelUsed());
        System.out.printf("  Latency    : %d ms%n", res.latencyMs());
        System.out.printf("  Cost       : $%.6f USD (Saved: $%.6f USD)%n", res.costUsd(), res.costSavedUsd());
        System.out.printf("  Output     : %s%n%n", res.response());
    }
}
