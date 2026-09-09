package com.genai.enterprise.graduation;

import java.util.ArrayList;
import java.util.List;

/**
 * Enterprise Production Readiness Auditor verifying 15 critical operational controls.
 */
public class ProductionReadinessAuditor {

    public List<ProductionChecklistItem> auditSystem() {
        List<ProductionChecklistItem> items = new ArrayList<>();

        // 1. Security & Identity
        items.add(new ProductionChecklistItem("SECURITY", "JWT / RBAC Enforcement",
                "API endpoints verify cryptographic JWT signatures and enforce method-level RBAC.",
                true, "Ensure @PreAuthorize is present on all financial and administrative endpoints."));

        items.add(new ProductionChecklistItem("SECURITY", "Prompt Injection Defense",
                "Multi-layer regex and heuristic filters block adversarial system overrides and prompt leaks.",
                true, "Maintain updated jailbreak signatures and employ canary token traps."));

        items.add(new ProductionChecklistItem("SECURITY", "PII & DLP Masking",
                "Bidirectional masking redacts SSNs and credit card numbers prior to third-party model egress.",
                true, "Configure strict regex patterns and DLP interceptors in the gateway."));

        // 2. Observability & Cost
        items.add(new ProductionChecklistItem("OBSERVABILITY", "OpenTelemetry Gen AI Spans",
                "Traces record CNCF gen_ai.* conventions: model, input_tokens, output_tokens, latency.",
                true, "Verify OpenTelemetry Java agent or Micrometer Tracing bridge is enabled."));

        items.add(new ProductionChecklistItem("OBSERVABILITY", "Per-Tenant Financial Ledger",
                "Token consumption costs are calculated and billed to tenant accounts in real-time.",
                true, "Implement TokenCostCalculator in request interception filters."));

        items.add(new ProductionChecklistItem("OBSERVABILITY", "Prompt Redaction in Tracing",
                "spring.ai.chat.observations.include-prompt is disabled in production to prevent data leaks.",
                true, "Set include-prompt=false in production application.yml."));

        // 3. Caching & Rate Limiting
        items.add(new ProductionChecklistItem("PERFORMANCE", "Dual Token Bucket Rate Limiting",
                "Both Requests Per Minute (RPM) and Tokens Per Minute (TPM) limits are enforced.",
                true, "Deploy Bucket4j with Redis backing for multi-pod synchronization."));

        items.add(new ProductionChecklistItem("PERFORMANCE", "Multi-Tier Caching",
                "Exact SHA-256 caching and semantic vector caching return repetitive queries in < 20ms.",
                true, "Configure Redis cluster with allkeys-lru eviction policy."));

        items.add(new ProductionChecklistItem("PERFORMANCE", "Dynamic Model Routing",
                "Low-complexity queries route to Tier 2 (mini) or Tier 3 (local) saving 80%+ API costs.",
                true, "Route short factual lookups to Ollama Llama 3.2 or GPT-4o-mini."));

        // 4. Deployment & Infrastructure
        items.add(new ProductionChecklistItem("DEPLOYMENT", "Multi-Stage Distroless Docker Image",
                "Production container uses minimal JRE runtime and executes as non-root user (appuser).",
                true, "Use Eclipse Temurin 21 JRE Jammy with chown appuser:appgroup."));

        items.add(new ProductionChecklistItem("DEPLOYMENT", "Container-Aware JVM Tuning",
                "-XX:MaxRAMPercentage=75.0 and -XX:+UseZGC -XX:+ZGenerational are configured.",
                true, "Add MaxRAMPercentage=75.0 to container JAVA_OPTS."));

        items.add(new ProductionChecklistItem("DEPLOYMENT", "Graceful Shutdown for Streaming",
                "server.shutdown=graceful ensures active SSE connections finish before pod termination.",
                true, "Set server.shutdown=graceful with timeout-per-shutdown-phase=45s."));

        // 5. Quality & Resilience
        items.add(new ProductionChecklistItem("RESILIENCE", "Model Fallback Circuit Breaker",
                "Transient cloud LLM timeouts automatically fail over to local Ollama open-weight models.",
                true, "Wrap primary ChatModel calls with Resilience4j circuit breakers."));

        items.add(new ProductionChecklistItem("QUALITY", "Automated RAG Triad CI/CD Gates",
                "Maven build asserts Groundedness >= 0.85 and Answer Relevance >= 0.80 on pull requests.",
                true, "Add EvaluationSuite assertions into JUnit 5 test phase."));

        items.add(new ProductionChecklistItem("DATABASE", "HNSW Index on pgvector",
                "Vector database uses HNSW index with m=16, ef_construction=64 and tuned ef_search.",
                true, "Run CREATE INDEX CONCURRENTLY USING hnsw on pgvector embeddings."));

        return items;
    }
}
