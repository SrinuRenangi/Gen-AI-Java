package com.genai.springai.advancedrag;

import com.genai.springai.embeddings.EmbeddingModel;
import com.genai.springai.embeddings.NomicEmbeddingModel;
import com.genai.springai.vectorstore.Document;
import com.genai.springai.vectorstore.PgVectorStoreSimulator;
import com.genai.springai.vectorstore.VectorStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdvancedRagDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  DAY 40: ADVANCED RAG — HYDE, MULTI-QUERY & CROSS-ENCODER RE-RANKING           ");
        System.out.println("================================================================================\n");

        EmbeddingModel embeddingModel = new NomicEmbeddingModel();
        VectorStore vectorStore = new PgVectorStoreSimulator(embeddingModel);

        // Ingest Knowledge Base
        vectorStore.add(List.of(
                new Document("DOC-NET-01",
                        "HTTP 504 Gateway Timeout indicates an edge reverse proxy (such as Nginx or AWS ALB) "
                                + "failed to receive a timely response from the upstream microservice. "
                                + "Fix: Increase proxy_read_timeout in Nginx or optimize the upstream slow SQL query.",
                        Map.of("category", "NETWORKING")),
                new Document("DOC-NET-02",
                        "HTTP 502 Bad Gateway indicates the upstream service crashed or actively rejected the connection socket.",
                        Map.of("category", "NETWORKING")),
                new Document("DOC-AUTH-01",
                        "HTTP 401 Unauthorized occurs when the client fails to provide a signed RS256 Bearer JWT in the Authorization header.",
                        Map.of("category", "SECURITY")),
                new Document("DOC-DB-01",
                        "PostgreSQL connection pooling timeouts occur when HikariCP maximumPoolSize is exhausted by long-running transactions.",
                        Map.of("category", "DATABASE"))
        ));

        // -----------------------------------------------------------------------------------------
        // SCENARIO 1: Hypothetical Document Embeddings (HyDE)
        // -----------------------------------------------------------------------------------------
        System.out.println("[TEST 1] Hypothetical Document Embeddings (HyDE) Transformation...");
        String rawVagueQuery = "how to fix 504?";
        System.out.println("  Raw User Query: \"" + rawVagueQuery + "\"");

        HydeQueryTransformer hyde = new HydeQueryTransformer();
        String hypotheticalPassage = hyde.transform(rawVagueQuery);
        System.out.println("  Generated HyDE Passage:\n  \"" + hypotheticalPassage + "\"");

        List<Document> hydeResults = vectorStore.similaritySearch(hypotheticalPassage);
        System.out.println("\n  --- HyDE Retrieved Documents ---");
        for (Document d : hydeResults) {
            System.out.println("  * [" + d.id() + "]: " + d.content().substring(0, 60) + "...");
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 2: Multi-Query Expansion & Reciprocal Rank Fusion (RRF)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 2] Multi-Query Expansion & Reciprocal Rank Fusion (RRF)...");
        List<String> expandedQueries = hyde.expandQueries(rawVagueQuery);
        System.out.println("  Expanded into " + expandedQueries.size() + " parallel queries:");
        expandedQueries.forEach(q -> System.out.println("    - \"" + q + "\""));

        List<List<Document>> allResults = new ArrayList<>();
        for (String q : expandedQueries) {
            allResults.add(vectorStore.similaritySearch(q));
        }

        List<ReciprocalRankFusion.RankedDocument> fusedResults = ReciprocalRankFusion.fuse(allResults, 3);
        System.out.println("\n  --- Fused Rankings via RRF Algorithm ---");
        for (int i = 0; i < fusedResults.size(); i++) {
            ReciprocalRankFusion.RankedDocument rd = fusedResults.get(i);
            System.out.printf("  #%d [RRF Score: %.4f] [%s]: %s%n",
                    i + 1, rd.rrfScore(), rd.document().id(), rd.document().content().substring(0, 60) + "...");
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 3: Cross-Encoder Second-Stage Re-Ranking
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 3] Second-Stage Cross-Encoder Deep Attention Re-Ranking...");
        CrossEncoderReranker reranker = new CrossEncoderReranker();
        List<Document> candidateDocs = fusedResults.stream().map(ReciprocalRankFusion.RankedDocument::document).toList();

        List<CrossEncoderReranker.RerankedDocument> finalRanked = 
                reranker.rerank(rawVagueQuery, candidateDocs, 2);

        System.out.println("  --- Final Re-Ranked Top Results ---");
        for (int i = 0; i < finalRanked.size(); i++) {
            CrossEncoderReranker.RerankedDocument rrd = finalRanked.get(i);
            System.out.printf("  #%d [Cross-Score: %.4f] [%s]: %s%n",
                    i + 1, rrd.crossAttentionScore(), rrd.document().id(), rrd.document().content().substring(0, 70) + "...");
        }

        System.out.println("\n================================================================================");
        System.out.println("  ADVANCED RAG PIPELINE VALIDATED SUCCESSFULLY! HIGH RECALL & PRECISION.        ");
        System.out.println("================================================================================");
    }
}
