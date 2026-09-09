package com.genai.enterprise.observability;

import java.util.HashMap;
import java.util.Map;

/**
 * Enterprise token financial calculator matching industry pricing tables (per million tokens).
 */
public class TokenCostCalculator {

    public record ModelPricing(double inputPricePerMillion, double outputPricePerMillion) {}

    private static final Map<String, ModelPricing> PRICING_TABLE = new HashMap<>();

    static {
        // Industry pricing models ($ per 1,000,000 tokens)
        PRICING_TABLE.put("gpt-4o", new ModelPricing(5.00, 15.00));
        PRICING_TABLE.put("gpt-4o-mini", new ModelPricing(0.15, 0.60));
        PRICING_TABLE.put("claude-3-5-sonnet", new ModelPricing(3.00, 15.00));
        PRICING_TABLE.put("text-embedding-3-small", new ModelPricing(0.02, 0.00));
        PRICING_TABLE.put("llama-3.2-3b-local", new ModelPricing(0.00, 0.00)); // self-hosted zero marginal
    }

    public static double calculateCost(String modelName, int promptTokens, int completionTokens) {
        ModelPricing pricing = PRICING_TABLE.getOrDefault(modelName.toLowerCase(), new ModelPricing(2.00, 8.00));
        double inputCost = (promptTokens / 1_000_000.0) * pricing.inputPricePerMillion();
        double outputCost = (completionTokens / 1_000_000.0) * pricing.outputPricePerMillion();
        return inputCost + outputCost;
    }

    public static ModelPricing getPricing(String modelName) {
        return PRICING_TABLE.getOrDefault(modelName.toLowerCase(), new ModelPricing(2.00, 8.00));
    }
}
