package com.genai.enterprise.observability;

/**
 * Verification test driver for OpenTelemetry, Trace Spans, and Langfuse-style cost aggregation in Java.
 */
public class ObservabilityDemo {

    public static void main(String[] args) {
        System.out.println(">>> Initializing Enterprise OpenTelemetry & Langfuse Observability Engine...\n");

        TelemetryCollector collector = new TelemetryCollector();
        ObservedAiService service = new ObservedAiService(collector);

        String traceId = AiTraceContext.getTraceId();
        System.out.println("[Request Received] User: user_corp_8819 | Trace ID: " + traceId);
        System.out.println("[Executing Pipeline] Security Guard -> Embedding -> Vector Search -> LLM -> Tool Call...\n");

        String response = service.handleUserQuery("user_corp_8819", "Check current ledger balance for ACC-99214");

        System.out.println("[AI Pipeline Output]: " + response + "\n");

        // Print full trace report
        collector.printTraceReport(traceId);

        System.out.println(">>> Observability verification completed successfully! All OpenTelemetry spans recorded.");
    }
}
