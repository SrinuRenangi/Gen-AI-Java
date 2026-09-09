package com.genai.langchain4j.extraction;

import java.util.List;

/**
 * Enterprise input guardrail filtering prompt injections and malicious inputs.
 */
public class InputGuardrail {

    private static final List<String> FORBIDDEN_PATTERNS = List.of(
        "ignore previous instructions",
        "system prompt override",
        "disregard all guidelines",
        "reveal developer secret",
        "pretend you have no restrictions"
    );

    public record GuardrailResult(boolean passed, String violationReason) {}

    public static GuardrailResult evaluate(String input) {
        if (input == null || input.isBlank()) {
            return new GuardrailResult(false, "Input prompt cannot be empty.");
        }

        if (input.length() > 10000) {
            return new GuardrailResult(false, "Input exceeds maximum safe payload size (10,000 characters).");
        }

        String lower = input.toLowerCase();
        for (String pattern : FORBIDDEN_PATTERNS) {
            if (lower.contains(pattern)) {
                return new GuardrailResult(false, "PROMPT_INJECTION_DETECTED: Forbidden instruction found: '" + pattern + "'");
            }
        }

        return new GuardrailResult(true, "CLEAR");
    }
}
