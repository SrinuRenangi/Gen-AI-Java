package com.javagenai.day14;

import java.util.Map;

public class AIHealthReporter {

    public enum Status { HEALTHY, DEGRADED, DOWN }

    public static Status evaluateCluster(Map<String, Boolean> subsystemChecks) {
        if (!subsystemChecks.getOrDefault("vector_db", false)) {
            return Status.DOWN;
        }
        if (!subsystemChecks.getOrDefault("primary_llm", false)) {
            return Status.DEGRADED;
        }
        return Status.HEALTHY;
    }
}
