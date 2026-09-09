package com.javagenai.day14;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class SimulatedActuatorEndpoint {
    private final AtomicLong promptTokensCounter = new AtomicLong(0);
    private final AtomicLong completionTokensCounter = new AtomicLong(0);
    private final Map<String, Object> healthComponents = new HashMap<>();

    public void setComponentHealth(String componentName, String status, Map<String, Object> details) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", status);
        payload.put("details", details);
        healthComponents.put(componentName, payload);
    }

    public void recordTokens(long prompt, long completion) {
        promptTokensCounter.addAndGet(prompt);
        completionTokensCounter.addAndGet(completion);
    }

    public void printHealthEndpoint() {
        System.out.println("HTTP GET /actuator/health");
        System.out.println("{");
        System.out.println("  \"status\": \"UP\",");
        System.out.println("  \"components\": {");
        healthComponents.forEach((name, data) -> {
            System.out.printf("    \"%s\": %s,%n", name, data);
        });
        System.out.println("  }");
        System.out.println("}");
    }

    public void printMetricsEndpoint() {
        System.out.println("HTTP GET /actuator/metrics");
        System.out.println("{");
        System.out.printf("  \"ai.llm.tokens.prompt\": %,d,%n", promptTokensCounter.get());
        System.out.printf("  \"ai.llm.tokens.completion\": %,d,%n", completionTokensCounter.get());
        System.out.printf("  \"ai.llm.tokens.total\": %,d%n", promptTokensCounter.get() + completionTokensCounter.get());
        System.out.println("}");
    }
}
