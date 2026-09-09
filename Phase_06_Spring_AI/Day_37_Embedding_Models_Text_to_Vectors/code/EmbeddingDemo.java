package com.genai.springai.embeddings;

import java.util.List;

public class EmbeddingDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("  DAY 37: EMBEDDING MODELS & COSINE SIMILARITY SEARCH DEMONSTRATION             ");
        System.out.println("================================================================================\n");

        EmbeddingModel embeddingModel = new NomicEmbeddingModel();
        System.out.println("  Active Embedding Model: " + embeddingModel.getModelName());
        System.out.println("  Vector Dimensions:      " + embeddingModel.dimensions());

        // -----------------------------------------------------------------------------------------
        // SCENARIO 1: Vector Inspection & L2 Normalization
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 1] Generating Embedding Vector & Inspecting Magnitude...");
        float[] vector = embeddingModel.embed("Enterprise Java GenAI");
        double magnitude = VectorMath.magnitude(vector);
        System.out.printf("  Vector Length: %d floats | L2 Magnitude: %.4f%n", vector.length, magnitude);
        System.out.printf("  Sample Coordinates [0..4]: [%.4f, %.4f, %.4f, %.4f, %.4f]%n",
                vector[0], vector[1], vector[2], vector[3], vector[4]);

        // -----------------------------------------------------------------------------------------
        // SCENARIO 2: Cosine Similarity Pairwise Comparison
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 2] Pairwise Cosine Similarity Comparison:");
        float[] vecJava1 = embeddingModel.embed("Java 21 Virtual Threads and Project Loom");
        float[] vecJava2 = embeddingModel.embed("JVM concurrency and lightweight background tasks");
        float[] vecCookie = embeddingModel.embed("Baking chocolate chip cookies with organic butter");

        double simRelated = VectorMath.cosineSimilarity(vecJava1, vecJava2);
        double simUnrelated = VectorMath.cosineSimilarity(vecJava1, vecCookie);

        System.out.printf("  Similarity [Java Concurrency vs JVM Concurrency]: %.4f (High Relevance)%n", simRelated);
        System.out.printf("  Similarity [Java Concurrency vs Baking Cookies]:   %.4f (Low Relevance)%n", simUnrelated);

        // -----------------------------------------------------------------------------------------
        // SCENARIO 3: In-Memory Semantic Search Engine (Indexing & Querying)
        // -----------------------------------------------------------------------------------------
        System.out.println("\n[TEST 3] In-Memory Semantic Search Engine:");
        SemanticSearchEngine engine = new SemanticSearchEngine(embeddingModel);

        engine.addDocument("DOC-001", "Java 21 introduces Virtual Threads (Project Loom) to scale concurrent IO-bound web services.");
        engine.addDocument("DOC-002", "PostgreSQL pgvector extension enables fast cosine similarity search over embeddings.");
        engine.addDocument("DOC-003", "Baking delicious chocolate chip cookies requires unbleached flour, sugar, and vanilla extract.");
        engine.addDocument("DOC-004", "Spring AI provides portable abstractions for ChatClient and EmbeddingModel across clouds.");

        System.out.println("  Indexed " + engine.documentCount() + " documents into vector space.");

        String query = "How do I build scalable concurrent multithreaded systems in Java?";
        System.out.println("  Search Query: \"" + query + "\"");

        List<SemanticSearchEngine.SearchResult> results = engine.search(query, 3);
        System.out.println("\n--- Top Ranked Results by Semantic Cosine Similarity ---");
        for (int i = 0; i < results.size(); i++) {
            SemanticSearchEngine.SearchResult r = results.get(i);
            System.out.printf("  #%d [Score: %.4f] [%s]: %s%n",
                    i + 1, r.similarityScore(), r.document().id(), r.document().content());
        }

        System.out.println("\n================================================================================");
        System.out.println("  VECTOR EMBEDDINGS & COSINE SEARCH VALIDATED SUCCESSFULLY!                     ");
        System.out.println("================================================================================");
    }
}
