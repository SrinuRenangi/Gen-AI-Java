package com.genai.enterprise.observability;

import java.util.*;

/**
 * In-memory Telemetry collector aggregating traces, spans, latency breakdowns, and token costs.
 */
public class TelemetryCollector {

    private final List<AiSpan> completedSpans = Collections.synchronizedList(new ArrayList<>());

    public void recordSpan(AiSpan span) {
        completedSpans.add(span);
    }

    public List<AiSpan> getSpansForTrace(String traceId) {
        List<AiSpan> result = new ArrayList<>();
        for (AiSpan s : completedSpans) {
            if (s.getTraceId().equals(traceId)) {
                result.add(s);
            }
        }
        return result;
    }

    public void printTraceReport(String traceId) {
        List<AiSpan> spans = getSpansForTrace(traceId);
        if (spans.isEmpty()) {
            System.out.println("No spans found for trace: " + traceId);
            return;
        }

        System.out.println("==========================================================================");
        System.out.println("               ENTERPRISE AI OBSERVABILITY TRACE REPORT                  ");
        System.out.println("==========================================================================");
        System.out.println("Trace ID: " + traceId);
        System.out.println("Total Spans Recorded: " + spans.size());

        int totalPromptTokens = 0;
        int totalCompletionTokens = 0;
        double totalCostUsd = 0.0;
        long totalDurationMs = 0;

        for (AiSpan s : spans) {
            if (s.getParentSpanId() == null) {
                totalDurationMs = s.getDurationMs();
            }
            Object pTok = s.getAttributes().get("ai.prompt.tokens");
            Object cTok = s.getAttributes().get("ai.completion.tokens");
            Object cost = s.getAttributes().get("ai.cost.usd");

            if (pTok instanceof Number n) totalPromptTokens += n.intValue();
            if (cTok instanceof Number n) totalCompletionTokens += n.intValue();
            if (cost instanceof Number n) totalCostUsd += n.doubleValue();
        }

        System.out.println(String.format("Total End-to-End Latency: %d ms", totalDurationMs));
        System.out.println(String.format("Tokens Consumed: %d prompt + %d completion = %d total",
                totalPromptTokens, totalCompletionTokens, (totalPromptTokens + totalCompletionTokens)));
        System.out.println(String.format("Total Cost: $%.6f USD", totalCostUsd));
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("TRACE SPAN HIERARCHY (Langfuse / OpenTelemetry Waterfall):");

        // Build tree
        Map<String, List<AiSpan>> childrenByParent = new HashMap<>();
        List<AiSpan> rootSpans = new ArrayList<>();

        for (AiSpan s : spans) {
            if (s.getParentSpanId() == null) {
                rootSpans.add(s);
            } else {
                childrenByParent.computeIfAbsent(s.getParentSpanId(), k -> new ArrayList<>()).add(s);
            }
        }

        for (AiSpan root : rootSpans) {
            printSpanNode(root, childrenByParent, "", true);
        }

        System.out.println("==========================================================================\n");
    }

    private void printSpanNode(AiSpan span, Map<String, List<AiSpan>> children, String prefix, boolean isTail) {
        String connector = isTail ? "\\-- " : "+-- ";
        StringBuilder details = new StringBuilder();
        details.append(String.format("[%s] %d ms", span.getOperationName(), span.getDurationMs()));

        if (span.getAttributes().containsKey("ai.model")) {
            details.append(" | model=").append(span.getAttributes().get("ai.model"));
        }
        if (span.getAttributes().containsKey("ai.total.tokens")) {
            details.append(" | tokens=").append(span.getAttributes().get("ai.total.tokens"));
        }
        if (span.getAttributes().containsKey("ai.cost.usd")) {
            details.append(String.format(" | cost=$%.5f", (Double) span.getAttributes().get("ai.cost.usd")));
        }

        System.out.println(prefix + connector + details.toString());

        List<AiSpan> spanChildren = children.getOrDefault(span.getSpanId(), Collections.emptyList());
        for (int i = 0; i < spanChildren.size(); i++) {
            boolean childIsTail = (i == spanChildren.size() - 1);
            String childPrefix = prefix + (isTail ? "    " : "|   ");
            printSpanNode(spanChildren.get(i), children, childPrefix, childIsTail);
        }
    }
}
