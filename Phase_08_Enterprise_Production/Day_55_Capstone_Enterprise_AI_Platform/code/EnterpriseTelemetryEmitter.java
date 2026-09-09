package com.genai.enterprise.capstone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * OpenTelemetry and Langfuse compatible telemetry emitter for enterprise AI transactions.
 */
public class EnterpriseTelemetryEmitter {

    public record TraceEvent(
            String traceId,
            String tenantId,
            String userId,
            String operation,
            String model,
            long latencyMs,
            int totalTokens,
            double costUsd,
            String status
    ) {}

    private final List<TraceEvent> events = Collections.synchronizedList(new ArrayList<>());

    public String generateTraceId() {
        return "trace-" + UUID.randomUUID().toString().substring(0, 8);
    }

    public void emit(TraceEvent event) {
        events.add(event);
    }

    public List<TraceEvent> getEvents() {
        return Collections.unmodifiableList(events);
    }

    public void printSummary() {
        System.out.println("==========================================================================");
        System.out.println("            CAPSTONE PLATFORM TELEMETRY LEDGER (OpenTelemetry)            ");
        System.out.println("==========================================================================");
        double totalCost = 0.0;
        int totalTokens = 0;
        for (TraceEvent e : events) {
            System.out.printf("[%s] Tenant: %s | User: %s | Op: %s | Model: %s | %d ms | Tokens: %d | Cost: $%.5f | Status: %s%n",
                    e.traceId(), e.tenantId(), e.userId(), e.operation(), e.model(), e.latencyMs(), e.totalTokens(), e.costUsd(), e.status());
            totalCost += e.costUsd();
            totalTokens += e.totalTokens();
        }
        System.out.println("--------------------------------------------------------------------------");
        System.out.printf("AGGREGATE METRICS: Total Transactions: %d | Total Tokens: %d | Total Spend: $%.5f USD%n",
                events.size(), totalTokens, totalCost);
        System.out.println("==========================================================================\n");
    }
}
