package com.javagenai.day07;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;

public class BatchEmbeddingSimulator {

    public static Duration simulateBatchEmbedding(int documentCount, int delayMs) {
        Instant start = Instant.now();

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < documentCount; i++) {
                final int id = i;
                executor.submit(() -> {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ignored) {}
                    return "embedding_vector_" + id;
                });
            }
        }

        return Duration.between(start, Instant.now());
    }
}
