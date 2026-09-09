package com.genai.langchain4j.advancedrag;

import java.util.*;

/**
 * Re-ranking content retriever taking coarse bi-encoder candidate matches
 * and re-ordering them with precision cross-encoder scores.
 */
public class ReRankingContentRetriever {

    private final ScoringModel scoringModel;
    private final int topK;
    private final double minScore;

    public record ScoredCandidate(String text, double crossEncoderScore, int originalRank) implements Comparable<ScoredCandidate> {
        @Override
        public int compareTo(ScoredCandidate o) {
            return Double.compare(o.crossEncoderScore, this.crossEncoderScore); // descending
        }
    }

    public ReRankingContentRetriever(ScoringModel scoringModel, int topK, double minScore) {
        this.scoringModel = scoringModel;
        this.topK = topK;
        this.minScore = minScore;
    }

    public List<ScoredCandidate> rerank(String query, List<String> coarseCandidates) {
        List<ScoredCandidate> scoredList = new ArrayList<>();

        for (int rank = 0; rank < coarseCandidates.size(); rank++) {
            String text = coarseCandidates.get(rank);
            double score = scoringModel.score(query, text);
            if (score >= minScore) {
                scoredList.add(new ScoredCandidate(text, score, rank + 1));
            }
        }

        Collections.sort(scoredList);
        return scoredList.subList(0, Math.min(scoredList.size(), topK));
    }
}
