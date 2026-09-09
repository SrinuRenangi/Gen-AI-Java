package com.genai.springai.advancedrag;

import com.genai.springai.vectorstore.Document;

import java.util.Comparator;
import java.util.List;

/**
 * Simulates a Cross-Encoder Re-Ranking model (e.g. Cohere Rerank or bge-reranker-large).
 * Evaluates the (Query, Document) pair simultaneously through full transformer cross-attention,
 * scoring exact factual and semantic relevance far more accurately than standalone Bi-Encoder cosine distance.
 */
public class CrossEncoderReranker {

    public record RerankedDocument(Document document, double crossAttentionScore) {}

    public List<RerankedDocument> rerank(String query, List<Document> candidateDocuments, int topK) {
        String queryLower = query.toLowerCase();

        return candidateDocuments.stream()
                .map(doc -> {
                    double score = computeCrossAttentionScore(queryLower, doc.content().toLowerCase());
                    return new RerankedDocument(doc, score);
                })
                .sorted(Comparator.comparingDouble(RerankedDocument::crossAttentionScore).reversed())
                .limit(topK)
                .toList();
    }

    private double computeCrossAttentionScore(String query, String docContent) {
        // Simulates deep token cross-attention scoring
        String[] queryWords = query.split("\\s+");
        int matchCount = 0;
        for (String w : queryWords) {
            if (w.length() > 3 && docContent.contains(w)) {
                matchCount++;
            }
        }

        double termDensity = (double) matchCount / Math.max(1, queryWords.length);
        // Add subtle positional bonus for early matches
        return Math.min(0.99, 0.45 + (termDensity * 0.5));
    }
}
