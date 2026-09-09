package com.genai.enterprise.security;

import java.util.List;

/**
 * Output guardrail verifying that generated responses do not leak system secrets or canary tokens.
 */
public class OutputLeakageGuard {

    private final String canaryToken;
    private final List<String> forbiddenSecretPatterns = List.of(
        "sk-proj-",
        "AKIA",
        "ghp_",
        "password",
        "bearer"
    );

    public OutputLeakageGuard(String canaryToken) {
        this.canaryToken = canaryToken;
    }

    public record LeakageCheckResult(boolean safe, String violationDetail) {}

    public LeakageCheckResult verifyOutput(String modelOutput) {
        if (modelOutput == null || modelOutput.isBlank()) {
            return new LeakageCheckResult(true, "OK");
        }

        // 1. Canary Token Verification (Detects System Prompt Leaks)
        if (canaryToken != null && !canaryToken.isBlank() && modelOutput.contains(canaryToken)) {
            return new LeakageCheckResult(false, "CANARY_BREACH: Model leaked internal canary token embedded in system prompt!");
        }

        // 2. Secret Key Pattern Detection
        for (String pattern : forbiddenSecretPatterns) {
            if (modelOutput.contains(pattern)) {
                return new LeakageCheckResult(false, "SECRET_EXPOSURE: Model output contains pattern matching private credentials: '" + pattern + "'");
            }
        }

        return new LeakageCheckResult(true, "OK");
    }
}
