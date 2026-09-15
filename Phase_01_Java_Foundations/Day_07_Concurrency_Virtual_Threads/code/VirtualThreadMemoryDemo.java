package com.genai.foundations.day07;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Day 07: Concurrency, Multithreading, and Virtual Threads in Memory.
 * Demonstrates high-volume virtual threads, atomic hardware CAS, and non-pinning ReentrantLock.
 */
public class VirtualThreadMemoryDemo {

    // 1. Shared atomic counter: Thread-safe via hardware CPU CAS
    private static final AtomicInteger TOTAL_PROMPTS_PROCESSED = new AtomicInteger(0);

    // 2. Modern lock avoiding virtual thread pinning
    private static final ReentrantLock AUDIT_LOCK = new ReentrantLock();

    public static void main(String[] args) throws Exception {
        System.out.println("==================================================");
        System.out.println("   DAY 07: VIRTUAL THREADS & CONCURRENCY MEMORY   ");
        System.out.println("==================================================");

        int taskCount = 1000;
        System.out.println("Launching " + taskCount + " Virtual Threads on standard RAM...");

        // Executors.newVirtualThreadPerTaskExecutor() creates a lightweight user-space thread per task
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= taskCount; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    simulateAiApiCall(taskId);
                });
            }
        } // Executor close() waits for all 1,000 virtual threads to finish!

        System.out.println("All tasks completed successfully!");
        System.out.println("Total Prompts Processed (Atomic CAS): " + TOTAL_PROMPTS_PROCESSED.get());
        System.out.println("==================================================");
    }

    private static void simulateAiApiCall(int taskId) {
        try {
            // Simulates blocking I/O (e.g. 50ms network delay calling an LLM)
            // Under Virtual Threads, this UNMOUNTS the continuation stack onto the Heap!
            Thread.sleep(50);

            // Atomic CAS increment without locks
            TOTAL_PROMPTS_PROCESSED.incrementAndGet();

            // Safe locking without thread pinning
            AUDIT_LOCK.lock();
            try {
                if (taskId == 1 || taskId == 500 || taskId == 1000) {
                    System.out.println("   [Sample Audit] Task #" + taskId + " executed on: " + Thread.currentThread());
                }
            } finally {
                AUDIT_LOCK.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
