package com.javagenai.day14;

import java.util.Map;

public class ActuatorDemo {

    public static void main(String[] args) {
        System.out.println("==================================================");
        System.out.println("   DAY 14: SPRING BOOT ACTUATOR MONITORING DEMO   ");
        System.out.println("==================================================");

        SimulatedActuatorEndpoint actuator = new SimulatedActuatorEndpoint();

        // 1. Registering Health Components
        System.out.println("1. Registering AI Subsystem Health Indicators:");
        actuator.setComponentHealth("vectorDatabase", "UP", Map.of(
            "engine", "PostgreSQL pgvector",
            "index", "HNSW_INDEX_ONLINE",
            "ping_latency_ms", 6
        ));

        actuator.setComponentHealth("ollamaLocal", "UP", Map.of(
            "endpoint", "http://localhost:11434",
            "models_loaded", "llama3.2:latest"
        ));

        actuator.printHealthEndpoint();
        System.out.println();

        // 2. Cluster Health Evaluation
        System.out.println("2. Cluster Readiness Evaluation:");
        Map<String, Boolean> currentStatus = Map.of("vector_db", true, "primary_llm", true);
        System.out.println("   Cluster Overall Status: " + AIHealthReporter.evaluateCluster(currentStatus));
        System.out.println();

        // 3. Simulating Live Traffic Token Ingestion
        System.out.println("3. Recording Live Token Metrics via Micrometer:");
        actuator.recordTokens(12500, 3200);
        actuator.recordTokens(45000, 8900);
        actuator.printMetricsEndpoint();
        System.out.println("==================================================");
    }
}
