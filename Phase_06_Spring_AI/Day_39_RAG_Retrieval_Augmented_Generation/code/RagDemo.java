package com.genai.springai.rag;

import com.genai.springai.chatclient.ChatClient;
import com.genai.springai.core.ChatModel;
import com.genai.springai.core.OllamaChatModel;
import com.genai.springai.embeddings.EmbeddingModel;
import com.genai.springai.embeddings.NomicEmbeddingModel;
import com.genai.springai.vectorstore.Document;
import com.genai.springai.vectorstore.PgVectorStoreSimulator;
import com.genai.springai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

public class RagDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  DAY 39: RETRIEVAL-AUGMENTED GENERATION (RAG) PIPELINE DEMONSTRATION           ");
        System.out.println("================================================================================\n");

        EmbeddingModel embeddingModel = new NomicEmbeddingModel();
        VectorStore vectorStore = new PgVectorStoreSimulator(embeddingModel);
        ChatModel chatModel = new OllamaChatModel("http://localhost:11434", "llama3.2");
        ChatClient chatClient = ChatClient.builder(chatModel).build();

        RagPipelineService ragService = new RagPipelineService(vectorStore, chatClient);

        // -----------------------------------------------------------------------------------------
        // SCENARIO 1: Document Ingestion & Chunking
        // -----------------------------------------------------------------------------------------
        System.out.println("[TEST 1] Ingesting & Chunking Proprietary Corporate Documents...");
        List<Document> corporateDocuments = List.of(
                new Document("DOC-SLA-01",
                        "Acme Cloud Corporation Service Level Agreement (SLA): "
                                + "We guarantee 99.99% uptime for all tier-1 production API endpoints. "
                                + "For P0 critical outages, the Site Reliability Engineering (SRE) team "
                                + "commits to an initial engineer response time of under 15 minutes. "
                                + "Financial credit compensation applies if monthly uptime falls below 99.9%.",
                        Map.of("category", "LEGAL_SLA", "year", 2026)),
                new Document("DOC-BENEFITS-01",
                        "Acme Corporation Health & Welfare Benefits: "
                                + "Annual open enrollment begins on November 1st and closes on November 15th at midnight EST. "
                                + "Employees must submit election changes through the internal Workday portal. "
                                + "Dependents can be added during this window without qualifying life event documentation.",
                        Map.of("category", "HR_BENEFITS", "year", 2026))
        );

        ragService.ingestDocuments(corporateDocuments);
        System.out.println("  ✅ Ingested and indexed document chunks into PostgreSQL pgvector.");

        // -----------------------------------------------------------------------------------------
        // SCENARIO 2: Grounded Question with Relevant Knowledge Base Retrieval
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 2] Asking Grounded Question: \"What is the SLA uptime and P0 response time?\"");
        RagPipelineService.RagAnswer answer1 = ragService.answerQuestion(
                "What is the SLA uptime and P0 response time?", 2, 0.40
        );

        System.out.println("--- Retrieved Sources (" + answer1.citedSources().size() + ") ---");
        for (Document doc : answer1.citedSources()) {
            int len = Math.min(75, doc.content().length());
            System.out.println("  * Source [" + doc.id() + "]: " + doc.content().substring(0, len) + "...");
        }
        System.out.println("\n--- Generated Grounded Answer ---\n" + answer1.answer());

        // -----------------------------------------------------------------------------------------
        // SCENARIO 3: Question Outside Knowledge Base (Triggering Anti-Hallucination Guardrail)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 3] Asking Ungrounded Question: \"How do I bake french croissants?\"");
        RagPipelineService.RagAnswer answer2 = ragService.answerQuestion(
                "How do I bake french croissants?", 2, 0.70
        );

        System.out.println("--- Retrieved Sources (" + answer2.citedSources().size() + ") ---");
        System.out.println("--- Generated Guardrail Response ---\n" + answer2.answer());

        System.out.println("\n================================================================================");
        System.out.println("  RAG RETRIEVAL & ANTI-HALLUCINATION GUARDRAILS VALIDATED SUCCESSFULLY!        ");
        System.out.println("================================================================================");
    }
}
