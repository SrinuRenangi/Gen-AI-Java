package com.genai.enterprise.deployment;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simulates Spring Boot Actuator Liveness and Readiness Health Indicators
 * used by Kubernetes deployment probes.
 */
public class HealthProbeSimulator {

    public enum Status { UP, DOWN, OUT_OF_SERVICE }

    public record HealthResult(Status status, Map<String, Object> details) {}

    private boolean databaseHealthy = true;
    private boolean pgvectorIndexLoaded = true;
    private boolean redisCacheAvailable = true;
    private boolean modelGatewayReachable = true;

    public HealthResult checkLiveness() {
        // Liveness verifies the JVM process is non-deadlocked and responsive
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("jvm.uptimeMs", 42500L);
        details.put("jvm.threads.active", Thread.activeCount());
        details.put("jvm.memory.freeBytes", Runtime.getRuntime().freeMemory());
        return new HealthResult(Status.UP, details);
    }

    public HealthResult checkReadiness() {
        // Readiness verifies all dependencies required to serve AI requests are ready
        Map<String, Object> details = new LinkedHashMap<>();
        boolean allReady = true;

        // 1. PostgreSQL DB Check
        details.put("db.postgresql", databaseHealthy ? "UP" : "DOWN");
        if (!databaseHealthy) allReady = false;

        // 2. pgvector Extension Check
        details.put("db.pgvector.hnsw_index", pgvectorIndexLoaded ? "READY" : "BUILDING");
        if (!pgvectorIndexLoaded) allReady = false;

        // 3. Redis Cache Check
        details.put("cache.redis", redisCacheAvailable ? "UP" : "DOWN");
        if (!redisCacheAvailable) allReady = false;

        // 4. LLM API Gateway Check
        details.put("llm.gateway", modelGatewayReachable ? "REACHABLE" : "UNREACHABLE");
        if (!modelGatewayReachable) allReady = false;

        Status overall = allReady ? Status.UP : Status.DOWN;
        return new HealthResult(overall, details);
    }

    public void setDatabaseHealthy(boolean databaseHealthy) { this.databaseHealthy = databaseHealthy; }
    public void setPgvectorIndexLoaded(boolean pgvectorIndexLoaded) { this.pgvectorIndexLoaded = pgvectorIndexLoaded; }
    public void setRedisCacheAvailable(boolean redisCacheAvailable) { this.redisCacheAvailable = redisCacheAvailable; }
    public void setModelGatewayReachable(boolean modelGatewayReachable) { this.modelGatewayReachable = modelGatewayReachable; }
}
