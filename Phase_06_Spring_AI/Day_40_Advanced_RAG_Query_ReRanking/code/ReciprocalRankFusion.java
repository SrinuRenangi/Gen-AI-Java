package com.genai.springai.advancedrag;

import com.genai.springai.vectorstore.Document;

import java.util.*;

/**
 * Implements the Reciprocal Rank Fusion (RRF) algorithm.
 * Merges multiple ranked lists from parallel search queries into a unified, calibrated ranking:
 * RRF_Score(d) = sum( 1 / (k + rank(d)) )
 */
public class ReciprocalRankFusion {

    private static final int DEFAULT_K = 60;

    public record RankedDocument(Document document, double rrfScore) {}

    public static List<RankedDocument> fuse(List<List<Document>> rankedLists, int topK) {
        Map<String, Document> docLookup = new HashMap<>();
        Map<String, Double> scoreMap = new HashMap<>();

        for (List<Document> list : rankedLists) {
            for (int rank = 0; rank < list.size(); rank++) {
                Document doc = list.get(rank);
                docLookup.put(doc.id(), doc);

                // RRF formula: 1.0 / (k + rank)
                double currentScore = scoreMap.getOrDefault(doc.id(), 0.0);
                double contribution = 1.0 / (DEFAULT_K + (rank + 1));
                scoreMap.put(doc.id(), currentScore + contribution);
            }
        }

        return scoreMap.entrySet().stream()
                .map(e -> new RankedDocument(docLookup.get(e.getKey()), e.getValue()))
                .sorted(Comparator.comparingDouble(RankedDocument::rrfScore).reversed())
                .limit(topK)
                .toList();
    }
}
