package com.genai.langchain4j.rag;

import java.util.List;
import java.util.Map;

/**
 * Executable demonstration of Day 46:
 * Complete RAG Pipeline in LangChain4j.
 */
public class LangChain4jRagDemo {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  DAY 46: LANGCHAIN4J RAG PIPELINE & RETRIEVAL AUGMENTATION DEMO ");
        System.out.println("==================================================================");

        // 1. Initialize Embedding Model & Store
        EmbeddingModel embeddingModel = new SimulatedEmbeddingModel();
        InMemoryEmbeddingStore store = new InMemoryEmbeddingStore();

        // 2. Ingest Enterprise Technical Documentation
        System.out.println("\n--- 1. Ingesting Enterprise Knowledge Base Documents ---");
        List<TextSegment> knowledgeBase = List.of(
            TextSegment.from(
                "Acme Cloud Kubernetes clusters enforce Pod Security Standards with restricted profiles by default.",
                Map.of("document", "k8s-security.pdf", "section", "PodSecurity")
            ),
            TextSegment.from(
                "Spring Boot microservices connect to pgvector database instances using HikariCP connection pools capped at 20 connections.",
                Map.of("document", "spring-database-guide.pdf", "section", "HikariPools")
            ),
            TextSegment.from(
                "Enterprise refund policy permits customer subscription reversals strictly within 30 calendar days of invoice dispatch.",
                Map.of("document", "corporate-policy.pdf", "section", "BillingRefunds")
            ),
            TextSegment.from(
                "Apache Kafka cluster partitions must be scaled to at least 12 partitions for high-throughput event topics.",
                Map.of("document", "kafka-operations.pdf", "section", "Partitioning")
            )
        );

        for (int i = 0; i < knowledgeBase.size(); i++) {
            TextSegment seg = knowledgeBase.get(i);
            Embedding emb = embeddingModel.embed(seg.text());
            store.add("doc-" + (i + 1), emb, seg);
            System.out.printf("   [Ingested doc-%d] from %s: \"%s\"\n",
                i + 1, seg.metadata().get("document"), seg.text().substring(0, Math.min(50, seg.text().length())) + "...");
        }

        System.out.println("Total Indexed Documents: " + store.size());

        // 3. Create ContentRetriever and RetrievalAugmentor
        ContentRetriever retriever = new EmbeddingStoreContentRetriever(store, embeddingModel, 2, 0.40);
        RetrievalAugmentor augmentor = new RetrievalAugmentor(retriever);

        // 4. Test Semantic Retrieval Query 1 (Database Connection Pools)
        System.out.println("\n--- 2. Executing Semantic Query on Database Policies ---");
        String query1 = "How many connections are configured for the Spring postgres database pool?";
        var augmentedResult1 = augmentor.augment(query1);

        System.out.println("Retrieved Sources Count: " + augmentedResult1.citedSources().size());
        for (TextSegment src : augmentedResult1.citedSources()) {
            System.out.println("   -> Matched Doc: " + src.metadata().get("document") + " | " + src.text());
        }

        System.out.println("\nGenerated Augmented Prompt Sent to LLM:\n");
        System.out.println(augmentedResult1.augmentedPrompt());

        // 5. Test Semantic Retrieval Query 2 (Corporate Refund Window)
        System.out.println("\n--- 3. Executing Semantic Query on Refund Timelines ---");
        String query2 = "What is our legal policy regarding customer subscription refunds?";
        var augmentedResult2 = augmentor.augment(query2);

        System.out.println("Retrieved Sources Count: " + augmentedResult2.citedSources().size());
        for (TextSegment src : augmentedResult2.citedSources()) {
            System.out.println("   -> Matched Doc: " + src.metadata().get("document") + " | " + src.text());
        }

        System.out.println("\n==================================================================");
        System.out.println("  LANGCHAIN4J RAG PIPELINE VERIFICATION COMPLETED SUCCESSFULLY   ");
        System.out.println("==================================================================");
    }
}
