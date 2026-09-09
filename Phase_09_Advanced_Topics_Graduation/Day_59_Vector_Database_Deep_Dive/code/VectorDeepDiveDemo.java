package com.genai.enterprise.vectordb;

import java.util.*;

/**
 * Verification test driver for Vector Database Deep Dive (HNSW vs Flat & Hybrid RRF Search) in Java 21.
 */
public class VectorDeepDiveDemo {

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("     DAY 59: VECTOR DATABASE DEEP DIVE (HNSW vs FLAT & HYBRID RRF)       ");
        System.out.println("==========================================================================\n");

        int dims = 32;
        int datasetSize = 100;
        Random rng = new Random(42);

        FlatVectorIndex flatIndex = new FlatVectorIndex();
        HnswGraphIndexSimulator.HnswConfig hnswConfig = new HnswGraphIndexSimulator.HnswConfig(16, 64, 50, 4);
        HnswGraphIndexSimulator hnswIndex = new HnswGraphIndexSimulator(hnswConfig);
        HybridSearchEngine hybridEngine = new HybridSearchEngine(new FlatVectorIndex());

        System.out.println("[Step 1: Indexing " + datasetSize + " High-Dimensional Vectors (" + dims + " dimensions)]");
        for (int i = 0; i < datasetSize; i++) {
            float[] vec = generateRandomVector(dims, rng);
            String doc = "Enterprise Knowledge Document #" + i + " regarding database performance and AI scaling.";
            VectorItem item = new VectorItem("DOC-" + i, doc, vec);
            flatIndex.add(item);
            hnswIndex.add(item);
            hybridEngine.addDocument(item);
        }
        System.out.println("  Flat Index Items: " + flatIndex.size() + " | HNSW Graph Nodes: " + hnswIndex.size() + "\n");

        // Query Vector
        float[] queryVec = generateRandomVector(dims, rng);
        int topK = 5;

        // 1. Flat Search Execution
        long t1 = System.nanoTime();
        var flatResults = flatIndex.search(queryVec, topK);
        long flatDurationNs = System.nanoTime() - t1;

        // 2. HNSW Search Execution
        long t2 = System.nanoTime();
        var hnswResults = hnswIndex.search(queryVec, topK);
        long hnswDurationNs = System.nanoTime() - t2;

        // Calculate Recall@K
        Set<String> groundTruthIds = new HashSet<>();
        for (var r : flatResults) groundTruthIds.add(r.item().id());

        long hits = hnswResults.stream().filter(r -> groundTruthIds.contains(r.item().id())).count();
        double recall = (double) hits / topK;

        System.out.println("[Step 2: Benchmark Comparison (Flat Exact k-NN vs HNSW Graph ANN)]");
        System.out.printf("  Flat Search Time : %d ns (Recall: 100.0%%)%n", flatDurationNs);
        System.out.printf("  HNSW Search Time : %d ns (Recall@%d: %.1f%%)%n", hnswDurationNs, topK, recall * 100.0);
        System.out.println("  Top Match (Flat) : " + flatResults.get(0).item().id() + " (Dist: " + String.format("%.4f", flatResults.get(0).distance()) + ")");
        if (!hnswResults.isEmpty()) {
            System.out.println("  Top Match (HNSW) : " + hnswResults.get(0).item().id() + " (Dist: " + String.format("%.4f", hnswResults.get(0).distance()) + ")\n");
        }

        // 3. Hybrid Search (RRF)
        System.out.println("[Step 3: Executing Enterprise Hybrid Search (Dense Vectors + Sparse BM25 via RRF)]");
        String textQuery = "Database scaling";
        var hybridResults = hybridEngine.search(textQuery, queryVec, topK);
        for (int i = 0; i < hybridResults.size(); i++) {
            var hr = hybridResults.get(i);
            System.out.printf("  Rank %d: %s | RRF Score: %.5f (Dense Rank: %d, Sparse Rank: %d)%n",
                    (i + 1), hr.item().id(), hr.rrfScore(), hr.denseRank(), hr.sparseRank());
        }

        System.out.println("\n==========================================================================");
        System.out.println(">>> Vector Database Deep Dive verification completed successfully!");
    }

    private static float[] generateRandomVector(int dims, Random rng) {
        float[] v = new float[dims];
        float norm = 0.0f;
        for (int i = 0; i < dims; i++) {
            v[i] = rng.nextFloat() * 2.0f - 1.0f;
            norm += v[i] * v[i];
        }
        norm = (float) Math.sqrt(norm);
        for (int i = 0; i < dims; i++) v[i] /= norm;
        return v;
    }
}
