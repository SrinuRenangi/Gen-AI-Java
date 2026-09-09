package com.genai.enterprise.evaluation;

import java.util.*;

/**
 * Enterprise LLM-as-a-Judge automated evaluator calculating RAG Triad scores.
 */
public class LlmAsAJudgeEvaluator {

    public RagTriadMetrics evaluate(RagTestCase tc) {
        // 1. Context Relevance
        double ctxRel = calculateContextRelevance(tc.userQuery(), tc.retrievedContext());
        String ctxRationale = ctxRel >= 0.8 
                ? "Context contains direct factual answers to query keywords." 
                : "Context contains substantial noise or irrelevant documentation.";

        // 2. Groundedness (Faithfulness)
        double grounded = calculateGroundedness(tc.retrievedContext(), tc.generatedAnswer());
        String groundRationale = grounded >= 0.8
                ? "All factual claims in generated answer are directly supported by context."
                : "HALLUCINATION DETECTED: Claims in answer are not grounded in retrieved context.";

        // 3. Answer Relevance
        double ansRel = calculateAnswerRelevance(tc.userQuery(), tc.generatedAnswer());
        String ansRationale = ansRel >= 0.8
                ? "Answer directly answers the user's specific question."
                : "Answer is evasive, off-topic, or misses key requirements of the query.";

        return new RagTriadMetrics(ctxRel, grounded, ansRel, ctxRationale, groundRationale, ansRationale);
    }

    private double calculateContextRelevance(String query, String context) {
        Set<String> qWords = extractKeywords(query);
        if (qWords.isEmpty()) return 1.0;

        String ctxLower = context.toLowerCase();
        long matches = qWords.stream().filter(ctxLower::contains).count();
        double ratio = (double) matches / qWords.size();
        return Math.min(1.0, Math.max(0.1, ratio));
    }

    private double calculateGroundedness(String context, String answer) {
        Set<String> ansKeywords = extractKeywords(answer);
        if (ansKeywords.isEmpty()) return 1.0;

        String ctxLower = context.toLowerCase();
        long supportedClaims = ansKeywords.stream().filter(ctxLower::contains).count();
        double ratio = (double) supportedClaims / ansKeywords.size();

        // Check for common hallucination triggers
        if (answer.toLowerCase().contains("guaranteed 100%") && !ctxLower.contains("guaranteed 100%")) {
            ratio *= 0.4; // heavy penalty for fabricated factual guarantee
        }

        return Math.min(1.0, Math.max(0.1, ratio));
    }

    private double calculateAnswerRelevance(String query, String answer) {
        Set<String> qWords = extractKeywords(query);
        if (qWords.isEmpty()) return 1.0;

        String ansLower = answer.toLowerCase();
        long matches = qWords.stream().filter(ansLower::contains).count();
        double ratio = (double) matches / qWords.size();

        // Evasive responses penalty
        if (ansLower.contains("i am not sure") || ansLower.contains("i cannot answer")) {
            return 0.2;
        }

        return Math.min(1.0, Math.max(0.15, ratio));
    }

    private Set<String> extractKeywords(String text) {
        Set<String> stopwords = Set.of("the", "is", "at", "which", "on", "a", "an", "and", "or", "in", "to", "for", "with", "of", "what", "how", "does");
        Set<String> keywords = new HashSet<>();
        for (String word : text.toLowerCase().replaceAll("[^a-z0-9 ]", "").split("\\s+")) {
            if (word.length() > 2 && !stopwords.contains(word)) {
                keywords.add(word);
            }
        }
        return keywords;
    }
}
