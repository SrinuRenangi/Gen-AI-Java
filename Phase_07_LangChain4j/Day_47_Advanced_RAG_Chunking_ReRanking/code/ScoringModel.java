package com.genai.langchain4j.advancedrag;

/**
 * Cross-Encoder Scoring Model interface.
 * Measures joint attention between full query and candidate text.
 * Matches dev.langchain4j.model.scoring.ScoringModel.
 */
public interface ScoringModel {
    double score(String query, String text);
}
