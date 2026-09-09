package com.genai.enterprise.observability;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * OpenTelemetry-compliant trace span recording hierarchical AI execution telemetry.
 */
public class AiSpan {

    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final String operationName;
    private final long startEpochMs;
    private long endEpochMs;
    private final Map<String, Object> attributes = new LinkedHashMap<>();

    public AiSpan(String traceId, String spanId, String parentSpanId, String operationName) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentSpanId = parentSpanId;
        this.operationName = operationName;
        this.startEpochMs = System.currentTimeMillis();
    }

    public AiSpan setAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    public void end() {
        this.endEpochMs = System.currentTimeMillis();
    }

    public long getDurationMs() {
        long end = (endEpochMs > 0) ? endEpochMs : System.currentTimeMillis();
        return end - startEpochMs;
    }

    public String getTraceId() { return traceId; }
    public String getSpanId() { return spanId; }
    public String getParentSpanId() { return parentSpanId; }
    public String getOperationName() { return operationName; }
    public long getStartEpochMs() { return startEpochMs; }
    public long getEndEpochMs() { return endEpochMs; }
    public Map<String, Object> getAttributes() { return Collections.unmodifiableMap(attributes); }
}
