package com.javagenai.day02;

import java.util.Objects;

public class AIModelSpecification {
    private final String modelId;
    private final int contextWindowTokens;
    private final double costPerMillionInputTokens;

    public AIModelSpecification(String modelId, int contextWindowTokens, double costPerMillionInputTokens) {
        if (modelId == null || modelId.isBlank()) {
            throw new IllegalArgumentException("modelId cannot be null or empty");
        }
        if (contextWindowTokens <= 0) {
            throw new IllegalArgumentException("contextWindowTokens must be positive. Received: " + contextWindowTokens);
        }
        if (costPerMillionInputTokens < 0) {
            throw new IllegalArgumentException("Cost cannot be negative. Received: " + costPerMillionInputTokens);
        }

        this.modelId = modelId;
        this.contextWindowTokens = contextWindowTokens;
        this.costPerMillionInputTokens = costPerMillionInputTokens;
    }

    public String getModelId() {
        return modelId;
    }

    public int getContextWindowTokens() {
        return contextWindowTokens;
    }

    public double getCostPerMillionInputTokens() {
        return costPerMillionInputTokens;
    }

    public double calculateInferenceCost(int inputTokens) {
        if (inputTokens < 0) {
            throw new IllegalArgumentException("Input tokens cannot be negative");
        }
        return (inputTokens / 1_000_000.0) * this.costPerMillionInputTokens;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AIModelSpecification that = (AIModelSpecification) o;
        return Objects.equals(modelId.toLowerCase(), that.modelId.toLowerCase());
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelId.toLowerCase());
    }

    @Override
    public String toString() {
        return String.format("AIModelSpecification[id='%s', contextWindow=%,d tokens, cost=$%.2f/1M]", 
                             modelId, contextWindowTokens, costPerMillionInputTokens);
    }
}
