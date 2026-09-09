package com.genai.langchain4j.advancedrag;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * Cross-encoder simulation computing joint attention, term overlap, and exact phrase matches.
 */
public class SimulatedCrossEncoderScoringModel implements ScoringModel {

    @Override
    public double score(String query, String text) {
        if (query == null || text == null || query.isBlank() || text.isBlank()) {
            return 0.0;
        }

        String qLower = query.toLowerCase();
        String tLower = text.toLowerCase();

        // Check for exact substring match
        double exactBonus = tLower.contains(qLower) ? 0.35 : 0.0;

        // Check for individual word overlaps
        String[] qWords = qLower.split("\\W+");
        Set<String> tWords = new HashSet<>(Arrays.asList(tLower.split("\\W+")));

        int matches = 0;
        for (String w : qWords) {
            if (w.length() > 2 && tWords.contains(w)) {
                matches++;
            }
        }

        double overlapRatio = (qWords.length > 0) ? (double) matches / qWords.length : 0.0;
        double finalScore = Math.min(1.0, (overlapRatio * 0.65) + exactBonus);

        return Math.round(finalScore * 1000.0) / 1000.0;
    }
}
