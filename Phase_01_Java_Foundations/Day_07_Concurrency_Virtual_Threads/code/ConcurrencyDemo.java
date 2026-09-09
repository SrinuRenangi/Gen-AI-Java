package com.javagenai.day07;

import java.time.Duration;

public class ConcurrencyDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 07: CONCURRENCY & VIRTUAL THREADS DEMO     ");
        System.out.println("==================================================");

        // 1. Thread-Safe Atomic Token Budget
        System.out.println("1. Thread-Safe Token Budget Management:");
        AtomicTokenBudget budget = new AtomicTokenBudget(10_000);

        boolean call1 = budget.tryConsume(4_000);
        boolean call2 = budget.tryConsume(5_500);
        boolean call3 = budget.tryConsume(1_500); // Exceeds budget (would be 11,000)!

        System.out.println("   Consume 4,000 tokens: " + (call1 ? "GRANTED" : "REJECTED"));
        System.out.println("   Consume 5,500 tokens: " + (call2 ? "GRANTED" : "REJECTED"));
        System.out.println("   Consume 1,500 tokens: " + (call3 ? "GRANTED" : "REJECTED (Budget Exceeded!)"));
        System.out.printf("   Total Tokens Consumed: %,d / 10,000%n", budget.getTokensConsumed());
        System.out.println();

        // 2. CompletableFuture Multi-Model Race
        System.out.println("2. Multi-Model Speculative Execution (CompletableFuture.anyOf):");
        String fastestAnswer = ModelRaceController.getFastestResponse("Summarize quarterly report");
        System.out.println("   Fastest Response Received: " + fastestAnswer);
        System.out.println();

        // 3. Virtual Thread Massive Concurrency Simulation
        System.out.println("3. Virtual Thread Batch Simulation (1,000 Concurrent I/O Tasks):");
        int taskCount = 1_000;
        int networkDelayMs = 150; // Each task simulates 150ms network delay
        System.out.printf("   Simulating %,d concurrent LLM tasks (each blocking for %dms)...%n", taskCount, networkDelayMs);

        Duration elapsed = BatchEmbeddingSimulator.simulateBatchEmbedding(taskCount, networkDelayMs);
        System.out.printf("   ✅ All %,d tasks finished in: %d ms!%n", taskCount, elapsed.toMillis());
        System.out.println("   (Notice: If run sequentially, 1,000 * 150ms = 150,000 ms / 2.5 minutes!)");
        System.out.println("==================================================");
    }
}
