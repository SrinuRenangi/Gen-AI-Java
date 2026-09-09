package com.javagenai.day13;

import java.util.function.Function;

public class AOPDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 13: ASPECT-ORIENTED PROGRAMMING (AOP) DEMO ");
        System.out.println("==================================================");

        // Core business logic (pure and unpolluted!)
        Function<String, String> rawAiService = prompt -> {
            try {
                Thread.sleep(85); // Simulated LLM inference
            } catch (InterruptedException ignored) {}
            return "[Generated summary for: " + prompt + "]";
        };

        // Aspect-wrapped service (simulating Spring AOP Proxy behavior)
        Function<String, String> proxiedAiService = SimulatedAOPProxy.wrapWithAspect("LLMSummarizationService", rawAiService);

        System.out.println("1. Invoking Proxied AI Service (Normal Flow):");
        String result = proxiedAiService.apply("Explain RAG pipeline architecture");
        System.out.println("Result: " + result);
        System.out.println();

        // Testing error flow
        System.out.println("2. Invoking Proxied AI Service (Simulated Error Flow):");
        Function<String, String> failingService = prompt -> {
            try { Thread.sleep(30); } catch (InterruptedException ignored) {}
            throw new IllegalStateException("OpenAI Gateway Rate Limit Exceeded (HTTP 429)");
        };

        Function<String, String> proxiedFailingService = SimulatedAOPProxy.wrapWithAspect("ResilienceAspect", failingService);
        try {
            proxiedFailingService.apply("Burst query");
        } catch (Exception expected) {
            System.out.println("Caught expected exception in caller: " + expected.getMessage());
        }
        System.out.println("==================================================");
    }
}
