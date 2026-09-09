package com.genai.enterprise.observability;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

/**
 * ThreadLocal stack manager maintaining trace and active span hierarchy.
 */
public class AiTraceContext {

    private static final ThreadLocal<String> currentTraceId = new ThreadLocal<>();
    private static final ThreadLocal<Deque<AiSpan>> spanStack = ThreadLocal.withInitial(ArrayDeque::new);

    public static String startTrace() {
        String newId = "tr-" + UUID.randomUUID().toString().substring(0, 8);
        startTrace(newId);
        return newId;
    }

    public static void startTrace(String traceId) {
        currentTraceId.set(traceId);
        spanStack.get().clear();
    }

    public static String getTraceId() {
        if (currentTraceId.get() == null) {
            startTrace();
        }
        return currentTraceId.get();
    }

    public static AiSpan startSpan(String operationName) {
        String traceId = getTraceId();
        Deque<AiSpan> stack = spanStack.get();
        String parentSpanId = stack.isEmpty() ? null : stack.peek().getSpanId();
        String spanId = "sp-" + UUID.randomUUID().toString().substring(0, 8);

        AiSpan span = new AiSpan(traceId, spanId, parentSpanId, operationName);
        stack.push(span);
        return span;
    }

    public static void endCurrentSpan(TelemetryCollector collector) {
        Deque<AiSpan> stack = spanStack.get();
        if (!stack.isEmpty()) {
            AiSpan span = stack.pop();
            span.end();
            if (collector != null) {
                collector.recordSpan(span);
            }
        }
    }

    public static void clear() {
        currentTraceId.remove();
        spanStack.remove();
    }
}
