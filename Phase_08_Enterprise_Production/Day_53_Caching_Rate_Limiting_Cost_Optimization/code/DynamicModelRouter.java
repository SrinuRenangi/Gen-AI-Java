package com.genai.enterprise.costopt;

import java.util.Set;

/**
 * Intelligent dynamic model router categorizing queries into tiered AI engines
 * to minimize enterprise token spend.
 */
public class DynamicModelRouter {

    public enum ModelTier {
        TIER_1_FRONTIER("gpt-4o", 5.00, 15.00),
        TIER_2_EFFICIENT("gpt-4o-mini", 0.15, 0.60),
        TIER_3_LOCAL_EDGE("llama-3.2-3b-local", 0.00, 0.00);

        private final String modelId;
        private final double inputCostPerMillion;
        private final double outputCostPerMillion;

        ModelTier(String modelId, double inputCost, double outputCost) {
            this.modelId = modelId;
            this.inputCostPerMillion = inputCost;
            this.outputCostPerMillion = outputCost;
        }

        public String getModelId() { return modelId; }
        public double getInputCostPerMillion() { return inputCostPerMillion; }
        public double getOutputCostPerMillion() { return outputCostPerMillion; }
    }

    public record RouteDecision(ModelTier selectedTier, String rationale, double estimatedCostUsd) {}

    private static final Set<String> HIGH_COMPLEXITY_KEYWORDS = Set.of(
            "architect", "synthesize", "refactor", "algorithm", "legal", "compliance",
            "security audit", "deadlock", "distributed consensus", "root cause analysis"
    );

    public static RouteDecision route(String prompt, int estimatedTokens) {
        String lower = prompt.toLowerCase();

        // 1. Frontier routing if reasoning or architectural complexity keywords are present
        for (String kw : HIGH_COMPLEXITY_KEYWORDS) {
            if (lower.contains(kw)) {
                double cost = calculateCost(ModelTier.TIER_1_FRONTIER, estimatedTokens);
                return new RouteDecision(ModelTier.TIER_1_FRONTIER, "High-complexity keyword detected: '" + kw + "'", cost);
            }
        }

        // 2. Long prompts (> 1200 characters) warrant Frontier model
        if (prompt.length() > 1200) {
            double cost = calculateCost(ModelTier.TIER_1_FRONTIER, estimatedTokens);
            return new RouteDecision(ModelTier.TIER_1_FRONTIER, "Prompt size (" + prompt.length() + " chars) requires high-context reasoning", cost);
        }

        // 3. Simple factual or short queries can be handled by Local Edge
        if (prompt.length() < 60 && !lower.contains("code") && !lower.contains("json")) {
            double cost = calculateCost(ModelTier.TIER_3_LOCAL_EDGE, estimatedTokens);
            return new RouteDecision(ModelTier.TIER_3_LOCAL_EDGE, "Short conversational query routed to zero-cost local LLM", cost);
        }

        // 4. Default standard queries routed to Efficient Tier (gpt-4o-mini)
        double cost = calculateCost(ModelTier.TIER_2_EFFICIENT, estimatedTokens);
        return new RouteDecision(ModelTier.TIER_2_EFFICIENT, "Standard query routed to Tier 2 (gpt-4o-mini, 97% cheaper)", cost);
    }

    private static double calculateCost(ModelTier tier, int tokens) {
        return (tokens / 1_000_000.0) * tier.getInputCostPerMillion();
    }
}
