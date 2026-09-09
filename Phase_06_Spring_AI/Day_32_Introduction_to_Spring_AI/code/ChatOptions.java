package com.genai.springai.core;

/**
 * Encapsulates model generation hyperparameters (temperature, topP, maxTokens).
 */
public record ChatOptions(
        String model,
        Double temperature,
        Double topP,
        Integer maxTokens
) {
    public static ChatOptions defaults() {
        return new ChatOptions("default-model", 0.7, 0.9, 1024);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model = "default-model";
        private Double temperature = 0.7;
        private Double topP = 0.9;
        private Integer maxTokens = 1024;

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public ChatOptions build() {
            return new ChatOptions(model, temperature, topP, maxTokens);
        }
    }
}
