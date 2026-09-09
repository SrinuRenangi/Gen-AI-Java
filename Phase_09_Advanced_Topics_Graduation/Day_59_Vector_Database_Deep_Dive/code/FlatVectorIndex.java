package com.genai.enterprise.vectordb;

import java.util.*;

/**
 * Exact Flat Vector Index (Brute-force k-NN linear scan).
 * Provides 100% ground-truth recall at O(N * D) complexity.
 */
public class FlatVectorIndex {

    public record SearchResult(VectorItem item, double distance) {}

    private final List<VectorItem> items = new ArrayList<>();

    public void add(VectorItem item) {
        items.add(item);
    }

    public List<SearchResult> search(float[] queryVector, int topK) {
        PriorityQueue<SearchResult> pq = new PriorityQueue<>(
                Comparator.comparingDouble(SearchResult::distance).reversed()
        );

        for (VectorItem item : items) {
            double dist = DistanceMetric.cosineDistance(queryVector, item.embedding());
            pq.offer(new SearchResult(item, dist));
            if (pq.size() > topK) {
                pq.poll();
            }
        }

        List<SearchResult> results = new ArrayList<>();
        while (!pq.isEmpty()) {
            results.add(pq.poll());
        }
        Collections.reverse(results);
        return results;
    }

    public int size() { return items.size(); }
}
