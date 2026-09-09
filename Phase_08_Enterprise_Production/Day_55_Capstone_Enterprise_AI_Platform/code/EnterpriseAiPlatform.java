package com.genai.enterprise.capstone;

import java.util.Map;

/**
 * Enterprise AI Platform Orchestrator unifying Security, Guardrails,
 * Caching, RAG, MCP Tool Execution, and OpenTelemetry Observability.
 */
public class EnterpriseAiPlatform {

    public record PlatformResponse(
            String content,
            String traceId,
            long latencyMs,
            String source,
            double costUsd
    ) {}

    private final PlatformGuardrailFilter guardrailFilter;
    private final EnterpriseCacheManager cacheManager;
    private final EnterpriseMcpToolRegistry toolRegistry;
    private final EnterpriseTelemetryEmitter telemetryEmitter;

    public EnterpriseAiPlatform() {
        this.guardrailFilter = new PlatformGuardrailFilter();
        this.cacheManager = new EnterpriseCacheManager();
        this.toolRegistry = new EnterpriseMcpToolRegistry();
        this.telemetryEmitter = new EnterpriseTelemetryEmitter();
    }

    public PlatformResponse processRequest(
            EnterpriseSecurityContext sec,
            String prompt,
            String toolToInvoke,
            Map<String, Object> toolParams
    ) {
        long startMs = System.currentTimeMillis();
        String traceId = telemetryEmitter.generateTraceId();

        // 1. Quota & Identity Check
        if (sec.tokenQuotaRemaining() <= 0) {
            telemetryEmitter.emit(new EnterpriseTelemetryEmitter.TraceEvent(
                    traceId, sec.tenantId(), sec.userId(), "platform.chat", "none",
                    System.currentTimeMillis() - startMs, 0, 0.0, "QUOTA_EXHAUSTED"));
            throw new IllegalStateException("Tenant quota exhausted for tenant: " + sec.tenantId());
        }

        // 2. Input Guardrail Inspection
        var scan = guardrailFilter.scanPrompt(prompt);
        if (!scan.passed()) {
            telemetryEmitter.emit(new EnterpriseTelemetryEmitter.TraceEvent(
                    traceId, sec.tenantId(), sec.userId(), "platform.chat", "none",
                    System.currentTimeMillis() - startMs, 0, 0.0, "SECURITY_VIOLATION"));
            throw new SecurityException(scan.violationReason());
        }

        // 3. Multi-Tenant Exact Cache Check
        String cached = cacheManager.get(sec.tenantId(), scan.sanitizedPrompt());
        if (cached != null) {
            long latency = System.currentTimeMillis() - startMs;
            telemetryEmitter.emit(new EnterpriseTelemetryEmitter.TraceEvent(
                    traceId, sec.tenantId(), sec.userId(), "platform.chat", "cache",
                    latency, 0, 0.0, "CACHE_HIT"));
            return new PlatformResponse(cached, traceId, latency, "CACHE_HIT", 0.0);
        }

        // 4. RAG Knowledge Retrieval (Simulating pgvector cosine search)
        String ragContext = retrieveRelevantContext(scan.sanitizedPrompt());

        // 5. Tool Execution via MCP (if requested)
        String toolOutput = "";
        if (toolToInvoke != null && !toolToInvoke.isBlank()) {
            toolOutput = toolRegistry.invokeTool(sec, toolToInvoke, toolParams);
        }

        // 6. LLM Synthesis & Token Modeling
        String model = prompt.length() > 200 ? "gpt-4o" : "gpt-4o-mini";
        int promptTokens = 120 + scan.sanitizedPrompt().length() / 4;
        int completionTokens = 85;
        double cost = calculateCost(model, promptTokens, completionTokens);

        simulateLlmLatency(120);

        String rawOutput;
        if (!toolOutput.isEmpty()) {
            rawOutput = "Based on secure financial records: " + toolOutput + 
                    " [Context reference: " + ragContext + "]";
        } else {
            rawOutput = "Enterprise AI synthesis for '" + scan.sanitizedPrompt() + 
                    "' backed by corporate policy: " + ragContext;
        }

        // 7. Output Sanitization (PII DLP Filter)
        String finalOutput = guardrailFilter.sanitizeOutput(rawOutput);

        // 8. Populate Cache
        cacheManager.put(sec.tenantId(), scan.sanitizedPrompt(), finalOutput);

        // 9. Telemetry Emission
        long latency = System.currentTimeMillis() - startMs;
        telemetryEmitter.emit(new EnterpriseTelemetryEmitter.TraceEvent(
                traceId, sec.tenantId(), sec.userId(), "platform.chat", model,
                latency, promptTokens + completionTokens, cost, "SUCCESS"));

        return new PlatformResponse(finalOutput, traceId, latency, "LLM_GENERATED", cost);
    }

    private String retrieveRelevantContext(String query) {
        // Simulated pgvector HNSW cosine search against enterprise policy embeddings
        return "Corporate Policy Sec 4.12: High-net-worth portfolios require annual dual-custody verification.";
    }

    private double calculateCost(String model, int pTokens, int cTokens) {
        if ("gpt-4o".equals(model)) {
            return (pTokens / 1_000_000.0) * 5.00 + (cTokens / 1_000_000.0) * 15.00;
        } else {
            return (pTokens / 1_000_000.0) * 0.15 + (cTokens / 1_000_000.0) * 0.60;
        }
    }

    private void simulateLlmLatency(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public EnterpriseTelemetryEmitter getTelemetryEmitter() { return telemetryEmitter; }
    public EnterpriseCacheManager getCacheManager() { return cacheManager; }
}
