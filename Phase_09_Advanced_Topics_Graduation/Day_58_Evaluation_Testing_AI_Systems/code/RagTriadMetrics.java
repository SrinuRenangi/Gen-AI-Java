package com.genai.enterprise.evaluation;

/**
 * Standardized RAG Triad evaluation metrics (0.0 to 1.0 normalized scale).
 */
public record RagTriadMetrics(
        double contextRelevance,
        double groundedness,
        double answerRelevance,
        String contextRationale,
        String groundednessRationale,
        String answerRationale
) {
    public double getCompositeScore() {
        // Balanced harmonic mean preventing high scores if any single pillar fails
        if (contextRelevance == 0 || groundedness == 0 || answerRelevance == 0) return 0.0;
        return 3.0 / ((1.0 / contextRelevance) + (1.0 / groundedness) + (1.0 / answerRelevance));
    }

    public boolean isPassing(double minPillarThreshold) {
        return contextRelevance >= minPillarThreshold &&
               groundedness >= minPillarThreshold &&
               answerRelevance >= minPillarThreshold;
    }
}
