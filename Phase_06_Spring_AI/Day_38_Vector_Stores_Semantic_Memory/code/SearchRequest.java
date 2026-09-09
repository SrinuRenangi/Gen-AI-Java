package com.genai.springai.vectorstore;

import java.util.Map;
import java.util.function.Predicate;

/**
 * Simulates Spring AI's SearchRequest builder for vector similarity queries.
 */
public record SearchRequest(
        String query,
        int topK,
        double similarityThreshold,
        Predicate<Map<String, Object>> filterExpression
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String query = "";
        private int topK = 4;
        private double similarityThreshold = 0.0;
        private Predicate<Map<String, Object>> filterExpression = metadata -> true;

        public Builder query(String query) {
            this.query = query;
            return this;
        }

        public Builder topK(int topK) {
            this.topK = topK;
            return this;
        }

        public Builder similarityThreshold(double threshold) {
            this.similarityThreshold = threshold;
            return this;
        }

        public Builder filter(Predicate<Map<String, Object>> filter) {
            this.filterExpression = filter;
            return this;
        }

        public SearchRequest build() {
            return new SearchRequest(query, topK, similarityThreshold, filterExpression);
        }
    }
}
