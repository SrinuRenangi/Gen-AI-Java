package com.genai.enterprise.costopt;

import java.util.concurrent.atomic.AtomicLong;

/**
 * End-to-end Cost Optimization Engine combining Rate Limiting, Multi-Tier Caching,
 * and Dynamic Model Routing.
 */
public class CostOptimizationEngine {

    public record ExecutionResult(
            String response,
            String source, // "EXACT_CACHE", "SEMANTIC_CACHE", or "LLM_GENERATED"
            String modelUsed,
            long latencyMs,
            double costUsd,
            double costSavedUsd
    ) {}

    private final TokenBucketRateLimiter rateLimiter;
    private final ExactMatchPromptCache exactCache;
    private final SemanticVectorCache semanticCache;
    private final AtomicLong totalSavedMicroCents = new AtomicLong(0);

    public CostOptimizationEngine(long maxRpm, long maxTpm, double semanticThreshold) {
        this.rateLimiter = new TokenBucketRateLimiter(maxRpm, maxTpm);
        this.exactCache = new ExactMatchPromptCache(300_000); // 5 min TTL
        this.semanticCache = new SemanticVectorCache(semanticThreshold);
    }

    public ExecutionResult execute(String prompt) {
        long startMs = System.currentTimeMillis();
        int estimatedTokens = Math.max(20, prompt.length() / 4);

        // 1. Rate Limiting Check
        TokenBucketRateLimiter.RateLimitResult rateCheck = rateLimiter.tryAcquire(estimatedTokens);
        if (!rateCheck.allowed()) {
            throw new RuntimeException("HTTP 429 Too Many Requests: " + rateCheck.reason() +
                    " (Retry after " + rateCheck.retryAfterMs() + " ms)");
        }

        // 2. Exact Match Cache Check
        String exactMatch = exactCache.get(prompt);
        if (exactMatch != null) {
            long latency = System.currentTimeMillis() - startMs;
            double baselineCost = (estimatedTokens / 1_000_000.0) * 5.00; // Frontier price saved
            recordSavings(baselineCost);
            return new ExecutionResult(exactMatch, "EXACT_CACHE", "none", latency, 0.0, baselineCost);
        }

        // 3. Semantic Vector Cache Check
        float[] queryEmbedding = generateMockEmbedding(prompt);
        var semanticMatch = semanticCache.findSimilar(queryEmbedding);
        if (semanticMatch.isPresent()) {
            long latency = System.currentTimeMillis() - startMs;
            double baselineCost = (estimatedTokens / 1_000_000.0) * 5.00;
            recordSavings(baselineCost);
            // Also store in exact cache for future instant hits
            exactCache.put(prompt, semanticMatch.get().response());
            return new ExecutionResult(semanticMatch.get().response(), "SEMANTIC_CACHE", "none", latency, 0.0, baselineCost);
        }

        // 4. Dynamic Model Routing
        DynamicModelRouter.RouteDecision decision = DynamicModelRouter.route(prompt, estimatedTokens);
        
        // 5. LLM Call Execution
        String generatedResponse = simulateLlmCall(decision.selectedTier(), prompt);
        long latency = System.currentTimeMillis() - startMs;

        // Calculate actual cost vs Frontier baseline cost
        double actualCost = decision.estimatedCostUsd();
        double frontierBaseline = (estimatedTokens / 1_000_000.0) * 5.00;
        double saved = Math.max(0.0, frontierBaseline - actualCost);
        recordSavings(saved);

        // 6. Populate Caches
        exactCache.put(prompt, generatedResponse);
        semanticCache.put(prompt, queryEmbedding, generatedResponse);

        return new ExecutionResult(generatedResponse, "LLM_GENERATED", decision.selectedTier().getModelId(),
                latency, actualCost, saved);
    }

    private String simulateLlmCall(DynamicModelRouter.ModelTier tier, String prompt) {
        try {
            // Tier 3 is fast local (30ms), Tier 2 is efficient (100ms), Tier 1 is heavy (250ms)
            long sleep = switch (tier) {
                case TIER_3_LOCAL_EDGE -> 30;
                case TIER_2_EFFICIENT -> 100;
                case TIER_1_FRONTIER -> 250;
            };
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "Synthesized answer to [" + prompt + "] using " + tier.getModelId();
    }

    /**
     * Deterministic pseudo-embedding for testing vector similarity.
     * Maps vocabulary overlap to high cosine similarity.
     */
    public static float[] generateMockEmbedding(String text) {
        float[] vec = new float[16];
        String[] words = text.toLowerCase().replaceAll("[^a-zA-Z0-9 ]", "").split("\\s+");
        for (String w : words) {
            int hash = Math.abs(w.hashCode());
            vec[hash % 16] += 1.0f;
        }
        // Normalize
        float norm = 0.0f;
        for (float v : vec) norm += v * v;
        if (norm > 0) {
            norm = (float) Math.sqrt(norm);
            for (int i = 0; i < vec.length; i++) vec[i] /= norm;
        }
        return vec;
    }

    private void recordSavings(double usd) {
        totalSavedMicroCents.addAndGet((long) (usd * 1_000_000.0));
    }

    public double getTotalSavedUsd() {
        return totalSavedMicroCents.get() / 1_000_000.0;
    }

    public ExactMatchPromptCache getExactCache() { return exactCache; }
    public SemanticVectorCache getSemanticCache() { return semanticCache; }
}
