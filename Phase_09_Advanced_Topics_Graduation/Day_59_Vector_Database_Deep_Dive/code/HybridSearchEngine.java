package com.genai.enterprise.vectordb;

import java.util.*;

/**
 * Enterprise Hybrid Search Engine combining Dense Vector Retrieval with Sparse BM25
 * using Reciprocal Rank Fusion (RRF).
 */
public class HybridSearchEngine {

    public record HybridResult(VectorItem item, double rrfScore, int denseRank, int sparseRank) {}

    private final FlatVectorIndex vectorIndex;
    private final List<VectorItem> documents = new ArrayList<>();
    private static final int RRF_K = 60; // Standard constant in RRF literature

    public HybridSearchEngine(FlatVectorIndex vectorIndex) {
        this.vectorIndex = vectorIndex;
    }

    public void addDocument(VectorItem item) {
        documents.add(item);
        vectorIndex.add(item);
    }

    public List<HybridResult> search(String queryText, float[] queryVector, int topK) {
        // 1. Dense Vector Search Ranking
        List<FlatVectorIndex.SearchResult> denseResults = vectorIndex.search(queryVector, documents.size());
        Map<String, Integer> denseRanks = new HashMap<>();
        for (int i = 0; i < denseResults.size(); i++) {
            denseRanks.put(denseResults.get(i).item().id(), i + 1);
        }

        // 2. Sparse Lexical Search Ranking (BM25 / Keyword Overlap)
        List<VectorItem> sparseResults = new ArrayList<>(documents);
        Set<String> queryWords = extractWords(queryText);
        sparseResults.sort((a, b) -> {
            long matchesB = extractWords(b.textPayload()).stream().filter(queryWords::contains).count();
            long matchesA = extractWords(a.textPayload()).stream().filter(queryWords::contains).count();
            return Long.compare(matchesB, matchesA);
        });
        Map<String, Integer> sparseRanks = new HashMap<>();
        for (int i = 0; i < sparseResults.size(); i++) {
            sparseRanks.put(sparseResults.get(i).id(), i + 1);
        }

        // 3. Reciprocal Rank Fusion (RRF)
        Map<String, Double> rrfScores = new HashMap<>();
        Map<String, VectorItem> itemMap = new HashMap<>();
        for (VectorItem it : documents) {
            itemMap.put(it.id(), it);
            int rDense = denseRanks.getOrDefault(it.id(), 1000);
            int rSparse = sparseRanks.getOrDefault(it.id(), 1000);
            double score = (1.0 / (RRF_K + rDense)) + (1.0 / (RRF_K + rSparse));
            rrfScores.put(it.id(), score);
        }

        // Sort by RRF score descending
        List<HybridResult> finalResults = new ArrayList<>();
        for (String id : rrfScores.keySet()) {
            finalResults.add(new HybridResult(
                    itemMap.get(id),
                    rrfScores.get(id),
                    denseRanks.getOrDefault(id, -1),
                    sparseRanks.getOrDefault(id, -1)
            ));
        }
        finalResults.sort(Comparator.comparingDouble(HybridResult::rrfScore).reversed());

        return finalResults.subList(0, Math.min(topK, finalResults.size()));
    }

    private Set<String> extractWords(String text) {
        return new HashSet<>(Arrays.asList(text.toLowerCase().replaceAll("[^a-z0-9 ]", "").split("\\s+")));
    }
}
