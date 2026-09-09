package com.javagenai.day04;

import java.util.*;

public class CollectionsDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 04: GENERICS & DATA STRUCTURES FOR AI      ");
        System.out.println("==================================================");

        // 1. Generic AIResponse
        AIResponse<String> textResponse = new AIResponse<>("Retrieval Augmented Generation complete.", 150, 45);
        System.out.println("1. Generic AIResponse Container:");
        System.out.println("   " + textResponse);
        System.out.println();

        // 2. Set Deduplication of Document Chunks
        System.out.println("2. Document Deduplication with LinkedHashSet:");
        List<String> rawChunks = List.of(
            "Java 21 introduces Virtual Threads.",
            "Spring AI supports pgvector.",
            "Java 21 introduces Virtual Threads.", // Duplicate chunk
            "PostgreSQL pgvector enables similarity search."
        );
        Set<String> uniqueChunks = new LinkedHashSet<>(rawChunks);
        System.out.printf("   Raw Chunks: %d, Unique Chunks: %d%n", rawChunks.size(), uniqueChunks.size());
        uniqueChunks.forEach(chunk -> System.out.println("   - " + chunk));
        System.out.println();

        // 3. Document Tag Index with HashMap & HashSet
        System.out.println("3. Tag Indexing via Map<String, Set<String>>:");
        DocumentTagIndex tagIndex = new DocumentTagIndex();
        tagIndex.addDocument("doc_001", List.of("Spring", "AI", "Enterprise"));
        tagIndex.addDocument("doc_002", List.of("PostgreSQL", "Database", "Enterprise"));
        tagIndex.addDocument("doc_003", List.of("Spring", "Security"));

        System.out.println("   Documents tagged 'Enterprise': " + tagIndex.findDocumentsByTag("Enterprise"));
        System.out.println("   Documents tagged 'Spring'    : " + tagIndex.findDocumentsByTag("Spring"));
        System.out.println();

        // 4. Top-K Selection via PriorityQueue (Min-Heap)
        System.out.println("4. Top-3 Semantic Matches via PriorityQueue (Min-Heap):");
        List<ScoredChunk> candidates = List.of(
            new ScoredChunk("Passage A: Virtual threads overview", 0.72),
            new ScoredChunk("Passage B: Pgvector cosine distance math", 0.94),
            new ScoredChunk("Passage C: Cooking recipes for pasta", 0.12),
            new ScoredChunk("Passage D: Spring AI ChatClient tutorial", 0.89),
            new ScoredChunk("Passage E: Deep learning backpropagation", 0.55),
            new ScoredChunk("Passage F: Advanced RAG re-ranking techniques", 0.91)
        );

        int k = 3;
        PriorityQueue<ScoredChunk> minHeap = new PriorityQueue<>(k);
        for (ScoredChunk candidate : candidates) {
            if (minHeap.size() < k) {
                minHeap.offer(candidate);
            } else if (candidate.similarityScore() > minHeap.peek().similarityScore()) {
                minHeap.poll();
                minHeap.offer(candidate);
            }
        }

        List<ScoredChunk> topResults = new ArrayList<>(minHeap);
        topResults.sort((a, b) -> Double.compare(b.similarityScore(), a.similarityScore()));

        topResults.forEach(r -> System.out.printf("   [Score: %.2f] %s%n", r.similarityScore(), r.text()));
        System.out.println("==================================================");
    }
}
