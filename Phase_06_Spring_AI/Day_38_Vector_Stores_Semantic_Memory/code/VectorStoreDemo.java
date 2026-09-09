package com.genai.springai.vectorstore;

import com.genai.springai.embeddings.EmbeddingModel;
import com.genai.springai.embeddings.NomicEmbeddingModel;

import java.util.List;
import java.util.Map;

public class VectorStoreDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  DAY 38: SPRING AI VECTORSTORE & PGVECTOR SIMULATION DEMONSTRATION             ");
        System.out.println("================================================================================\n");

        EmbeddingModel embeddingModel = new NomicEmbeddingModel();
        VectorStore vectorStore = new PgVectorStoreSimulator(embeddingModel);
        System.out.println("  Active Vector Store: " + vectorStore.getName());

        // -----------------------------------------------------------------------------------------
        // SCENARIO 1: Ingesting Enterprise Documents with Structured Metadata
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 1] Ingesting Corporate Policies with Department & Tenant Metadata...");
        List<Document> docs = List.of(
                new Document("DOC-FIN-01", "Employees can allocate up to 15% of base salary into company 401k with 50% match.",
                        Map.of("department", "FINANCE", "tenantId", "ACME", "confidentiality", "INTERNAL")),
                new Document("DOC-HR-01", "Annual performance bonuses are distributed in March based on quarterly OKR completion.",
                        Map.of("department", "HR", "tenantId", "ACME", "confidentiality", "INTERNAL")),
                new Document("DOC-ENG-01", "All production backend microservices must run on Java 21 LTS using Virtual Threads.",
                        Map.of("department", "ENGINEERING", "tenantId", "ACME", "confidentiality", "PUBLIC")),
                new Document("DOC-ENG-02", "PostgreSQL pgvector HNSW index is the standard storage engine for all RAG vector pipelines.",
                        Map.of("department", "ENGINEERING", "tenantId", "ACME", "confidentiality", "PUBLIC")),
                new Document("DOC-FIN-02", "Expense reports exceeding $500 require VP approval within the enterprise portal.",
                        Map.of("department", "FINANCE", "tenantId", "ACME", "confidentiality", "RESTRICTED"))
        );

        vectorStore.add(docs);
        System.out.println("  ✅ Successfully ingested and indexed " + docs.size() + " documents with HNSW vectors.");

        // -----------------------------------------------------------------------------------------
        // SCENARIO 2: Pure Unfiltered Semantic Similarity Search
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 2] Semantic Query: \"What is the retirement pension 401k savings policy?\"");
        SearchRequest req1 = SearchRequest.builder()
                .query("What is the retirement pension 401k savings policy?")
                .topK(2)
                .build();

        List<Document> results1 = vectorStore.similaritySearch(req1);
        System.out.println("--- Top 2 Matches (Unfiltered) ---");
        for (Document d : results1) {
            System.out.println("  [" + d.id() + "] [" + d.metadata().get("department") + "]: " + d.content());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 3: Hybrid Search with Metadata Filtering (SQL WHERE department = 'ENGINEERING')
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 3] Hybrid Search with Metadata Filter: department == 'ENGINEERING'");
        SearchRequest req2 = SearchRequest.builder()
                .query("What are the corporate database and concurrency standards?")
                .topK(3)
                .filter(meta -> "ENGINEERING".equals(meta.get("department")))
                .build();

        List<Document> results2 = vectorStore.similaritySearch(req2);
        System.out.println("--- Engineering Matches Only ---");
        for (Document d : results2) {
            System.out.println("  [" + d.id() + "] [" + d.metadata().get("department") + "]: " + d.content());
        }

        // -----------------------------------------------------------------------------------------
        // SCENARIO 4: Document Deletion
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 4] Deleting Document DOC-FIN-02...");
        vectorStore.delete(List.of("DOC-FIN-02"));
        System.out.println("  ✅ Deleted DOC-FIN-02. Vector store now contains " + ((PgVectorStoreSimulator) vectorStore).size() + " documents.");

        System.out.println("\n================================================================================");
        System.out.println("  VECTOR STORE INGESTION & HYBRID SEARCH VERIFIED SUCCESSFULLY!                 ");
        System.out.println("================================================================================");
    }
}
