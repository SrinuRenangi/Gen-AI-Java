package com.genai.foundations.day06;

import java.util.List;
import java.util.function.Predicate;

/**
 * Day 06: Functional Programming, Lambdas, and Streams in Memory.
 * Demonstrates non-capturing vs capturing lambdas, fused stream pipelines, short-circuiting, and IntStream primitive efficiency.
 */
record DocumentChunk(String id, String text, double relevanceScore, int tokens) {}

public class StreamMemoryDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 06: LAMBDAS & STREAM PIPELINE MEMORY TRACE ");
        System.out.println("==================================================");

        List<DocumentChunk> corpus = List.of(
            new DocumentChunk("c1", "Introduction to Generative AI in Java", 0.95, 120),
            new DocumentChunk("c2", "Ancient legacy COBOL architectures", 0.40, 85),
            new DocumentChunk("c3", "Vector database embeddings with pgvector", 0.91, 150),
            new DocumentChunk("c4", "Neural network quantization techniques", 0.88, 200)
        );

        // 1. Non-Capturing Lambda (Singleton in CallSite - 0 bytes Heap allocation)
        Predicate<DocumentChunk> nonCapturingFilter = chunk -> chunk.relevanceScore() >= 0.85;

        // 2. Capturing Lambda (Captures local stack variable 'minTokenLimit')
        int minTokenLimit = 100; // Local stack variable
        Predicate<DocumentChunk> capturingFilter = chunk -> chunk.tokens() >= minTokenLimit;

        // 3. Fused Stream Pipeline: Filter -> Map -> Limit -> toList
        List<String> topChunkTexts = corpus.stream()
            .filter(nonCapturingFilter) // Stage 1
            .filter(capturingFilter)    // Stage 2
            .map(DocumentChunk::text)   // Stage 3 (Method Reference)
            .limit(2)                   // Short-circuiting!
            .toList();                  // Terminal operation

        System.out.println("1. Top Selected Chunks (Fused Execution):");
        topChunkTexts.forEach(t -> System.out.println("   -> " + t));
        System.out.println();

        // 4. Primitive Stream (Zero Boxing Overhead)
        int totalTokens = corpus.stream()
            .mapToInt(DocumentChunk::tokens) // IntStream bypasses Integer auto-boxing!
            .sum();

        System.out.println("2. Total Corpus Tokens (Calculated via IntStream): " + totalTokens);
        System.out.println("==================================================");
    }
}
