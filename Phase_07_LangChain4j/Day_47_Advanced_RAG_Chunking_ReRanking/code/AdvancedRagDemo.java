package com.genai.langchain4j.advancedrag;

import java.util.List;

/**
 * Executable demonstration of Day 47:
 * Advanced RAG - Chunking, Scoring & Re-Ranking in LangChain4j.
 */
public class AdvancedRagDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  DAY 47: ADVANCED RAG - CHUNKING, SCORING & RE-RANKING DEMO     ");
        System.out.println("==================================================================");

        // 1. RECURSIVE CHUNKING WITH OVERLAP
        System.out.println("\n--- 1. Recursive Document Chunking with Sliding Overlap ---");
        String longTechnicalDoc = """
            Spring Boot 3.3 introduces enhanced virtual thread support for reactive and web frameworks.
            When running on OpenJDK 21, tomcat threads are dynamically mapped to virtual carriers.

            PostgreSQL vector extensions require tuned maintenance_work_mem settings.
            For databases with over 10 million vectors, HNSW index construction requires at least 4GB of RAM.

            Kafka event brokers require minimum in-sync replicas configured to two.
            This ensures zero message loss even during unplanned broker pod eviction.
            """;

        RecursiveDocumentSplitter splitter = new RecursiveDocumentSplitter(220, 50);
        List<String> chunks = splitter.split(longTechnicalDoc);

        System.out.println("Generated Chunks Count: " + chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            System.out.printf("   [Chunk %d (%d chars)]:\n   \"%s\"\n\n",
                i + 1, chunks.get(i).length(), chunks.get(i).replace("\n", " "));
        }

        // 2. CROSS-ENCODER RE-RANKING & RANK INVERSION
        System.out.println("--- 2. Cross-Encoder Re-Ranking (Rank Inversion Demonstration) ---");
        String userQuery = "How much RAM is required for PostgreSQL HNSW index creation?";

        // Coarse candidate matches returned by Stage 1 Bi-Encoder (approximate similarity)
        List<String> coarseCandidates = List.of(
            "Spring Boot 3.3 introduces enhanced virtual thread support for reactive frameworks.", // Bi-encoder rank 1 (coarse)
            "Kafka event brokers require minimum in-sync replicas configured to two.",              // Bi-encoder rank 2 (coarse)
            "For databases with over 10 million vectors, HNSW index construction requires at least 4GB of RAM." // Bi-encoder rank 3 (coarse)
        );

        System.out.println("Stage 1 Bi-Encoder Rankings (Coarse Vector Similarity):");
        for (int i = 0; i < coarseCandidates.size(); i++) {
            System.out.printf("   Initial Rank %d: \"%s\"\n", i + 1, coarseCandidates.get(i));
        }

        // Stage 2 Cross-Encoder Re-ranking
        ScoringModel scoringModel = new SimulatedCrossEncoderScoringModel();
        ReRankingContentRetriever reranker = new ReRankingContentRetriever(scoringModel, 2, 0.20);
        List<ReRankingContentRetriever.ScoredCandidate> reranked = reranker.rerank(userQuery, coarseCandidates);

        System.out.println("\nStage 2 Cross-Encoder Rankings (Deep Joint-Attention Re-Ranking):");
        for (int i = 0; i < reranked.size(); i++) {
            var c = reranked.get(i);
            System.out.printf("   New Rank %d (score: %.3f, was initial rank %d): \"%s\"\n",
                i + 1, c.crossEncoderScore(), c.originalRank(), c.text());
        }

        // 3. DYNAMIC QUERY ROUTING
        System.out.println("\n--- 3. Enterprise Dynamic Query Routing ---");
        QueryRouter router = new QueryRouter();

        String q1 = "How many pods should be provisioned for Kubernetes cluster autoscaling?";
        String q2 = "Can I carry over 5 days of unused PTO into the next calendar quarter?";
        String q3 = "What is the corporate tax deduction limit for employee travel meals?";

        System.out.println("Query 1: \"" + q1 + "\" -> Route to: " + router.route(q1));
        System.out.println("Query 2: \"" + q2 + "\" -> Route to: " + router.route(q2));
        System.out.println("Query 3: \"" + q3 + "\" -> Route to: " + router.route(q3));

        System.out.println("\n==================================================================");
        System.out.println("  ADVANCED RAG VERIFICATION COMPLETED SUCCESSFULLY               ");
        System.out.println("==================================================================");
    }
}
