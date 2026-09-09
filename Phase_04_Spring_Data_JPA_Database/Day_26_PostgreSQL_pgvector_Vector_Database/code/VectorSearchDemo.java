package code;

import java.util.List;

/**
 * Driver class demonstrating Day 26: PostgreSQL pgvector — Your Vector Database.
 *
 * Demonstrates:
 * 1. Vector Distance Mathematics (Cosine Distance <=> vs Euclidean Distance <->).
 * 2. Semantic Search on Document Chunks in PostgreSQL.
 * 3. Hybrid Filtering: Combining Tenant Relational Isolation with Vector Similarity.
 */
public class VectorSearchDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println(" DAY 26: POSTGRESQL PGVECTOR — HIGH-DIMENSIONAL VECTOR DATABASE & HYBRID SEARCH ");
        System.out.println("================================================================================");

        // -------------------------------------------------------------------------
        // SCENARIO 1: MATHEMATICAL VECTOR DISTANCES
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 1: Mathematical Vector Distance Foundations ---");

        // Simplified 4-dimensional embeddings representing semantic concept coordinates
        // Dimensions: [Concurrency, Networking, Baking, Sugar]
        float[] javaVirtualThreads = new float[]{ 0.95f,  0.88f, 0.02f, 0.00f };
        float[] goGoroutines        = new float[]{ 0.92f,  0.85f, 0.01f, 0.00f };
        float[] chocolateCake       = new float[]{ 0.01f,  0.00f, 0.94f, 0.89f };

        double distThreadsToGo = VectorMath.cosineDistance(javaVirtualThreads, goGoroutines);
        double simThreadsToGo  = VectorMath.cosineSimilarity(javaVirtualThreads, goGoroutines);

        double distThreadsToCake = VectorMath.cosineDistance(javaVirtualThreads, chocolateCake);
        double simThreadsToCake  = VectorMath.cosineSimilarity(javaVirtualThreads, chocolateCake);

        System.out.printf(" Concept 'Java Virtual Threads' vs 'Go Goroutines':%n");
        System.out.printf("   Cosine Distance (<=>): %.4f (Close to 0.0 = Highly Similar)%n", distThreadsToGo);
        System.out.printf("   Cosine Similarity:    %.4f (Close to 1.0 = Strong Semantic Alignment)%n%n", simThreadsToGo);

        System.out.printf(" Concept 'Java Virtual Threads' vs 'Chocolate Cake':%n");
        System.out.printf("   Cosine Distance (<=>): %.4f (Close to 1.0 = Orthogonal/Unrelated)%n", distThreadsToCake);
        System.out.printf("   Cosine Similarity:    %.4f%n", simThreadsToCake);

        // -------------------------------------------------------------------------
        // SCENARIO 2 & 3: PGVECTOR DATABASE SIMULATION WITH HYBRID TENANT SEARCH
        // -------------------------------------------------------------------------
        System.out.println("\n--- SCENARIO 2 & 3: pgvector Database Hybrid Nearest-Neighbor Search ---");

        PgVectorSimulator db = new PgVectorSimulator();

        // Populate database chunks for two enterprise tenants
        db.insert(1L, "doc_jvm_perf", "tenant_alpha", 
            "Java 21 introduces Virtual Threads to revolutionize concurrent high-throughput I/O.",
            new float[]{ 0.94f, 0.87f, 0.02f, 0.01f });

        db.insert(2L, "doc_go_runtime", "tenant_alpha", 
            "Go goroutines provide lightweight cooperative multi-tasking managed by the Go runtime.",
            new float[]{ 0.91f, 0.84f, 0.01f, 0.00f });

        db.insert(3L, "doc_baking_101", "tenant_alpha", 
            "Preheat oven to 350 degrees and whisk flour, cocoa powder, and sugar together.",
            new float[]{ 0.01f, 0.00f, 0.93f, 0.88f });

        // Chunk belonging to a different tenant (tenant_beta)
        db.insert(4L, "doc_secret_concurrency", "tenant_beta", 
            "Tenant Beta Confidential: Advanced Project Loom benchmark results.",
            new float[]{ 0.96f, 0.89f, 0.02f, 0.00f });

        System.out.println(" Populated database with " + db.size() + " document chunks across 2 tenants.");

        // Query Vector: User searches: "lightweight thread scheduling in modern runtimes"
        float[] queryVector = new float[]{ 0.93f, 0.86f, 0.01f, 0.00f };

        System.out.println("\n Executing Hybrid Query for 'tenant_alpha' (Top 2 Results):");
        List<PgVectorSimulator.SearchResult> results = db.searchNearestNeighbors("tenant_alpha", queryVector, 2);

        System.out.println("\n Query Results Returned by pgvector:");
        for (int i = 0; i < results.size(); i++) {
            PgVectorSimulator.SearchResult res = results.get(i);
            System.out.printf("   [%d] Doc: %-15s | Cosine Dist: %.4f | Similarity: %.2f%%%n",
                i + 1, res.chunk().documentId(), res.cosineDistance(), res.cosineSimilarity() * 100);
            System.out.printf("       Content: \"%s\"%n", res.chunk().content());
        }

        // Verify tenant isolation
        boolean leakedBeta = results.stream().anyMatch(r -> "tenant_beta".equals(r.chunk().tenantId()));
        System.out.println("\n Tenant Isolation Verification: Did any tenant_beta rows leak? " + leakedBeta);
        if (leakedBeta) {
            throw new AssertionError("Security Breach: Tenant isolation failed in hybrid vector search!");
        }

        System.out.println("\n================================================================================");
        System.out.println(" DAY 26 DEMONSTRATION COMPLETE: PGVECTOR SEMANTIC & HYBRID SEARCH VERIFIED!     ");
        System.out.println("================================================================================");
        System.out.println(" PHASE 4 COMPLETE: ALL SPRING DATA JPA & DATABASE MASTERY COMPLETE!             ");
        System.out.println("================================================================================");
    }
}
